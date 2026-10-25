package com.uniflow.academic.task.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.uniflow.academic.task.application.services.TaskReminders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;

class TaskRemindersTest {

    private final RecordingReminderScheduler scheduler = new RecordingReminderScheduler();
    private final TaskReminders reminders = new TaskReminders(scheduler, Runnable::run);
    private final Logger logger = (Logger) LoggerFactory.getLogger(TaskReminders.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runsImmediatelyWithoutATransaction() {
        reminders.schedule(aTask().build());
        reminders.reschedule(aTask().build());
        reminders.cancel("t1");

        assertThat(scheduler.scheduled).hasSize(1);
        assertThat(scheduler.rescheduled).hasSize(1);
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void defersUntilAfterCommitInsideATransaction() {
        TransactionSynchronizationManager.initSynchronization();
        reminders.schedule(aTask().build());
        reminders.reschedule(aTask().build());
        reminders.cancel("t1");

        assertThat(scheduler.scheduled).isEmpty();
        assertThat(scheduler.rescheduled).isEmpty();
        assertThat(scheduler.cancelled).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(scheduler.scheduled).hasSize(1);
        assertThat(scheduler.rescheduled).hasSize(1);
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void neverRunsWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        reminders.schedule(aTask().build());
        reminders.cancel("t1");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(scheduler.scheduled).isEmpty();
        assertThat(scheduler.cancelled).isEmpty();
    }

    @Test
    void failuresAreSwallowedInBothModes() {
        scheduler.failing = true;
        reminders.schedule(aTask().build());
        reminders.reschedule(aTask().build());
        reminders.cancel("t1");

        TransactionSynchronizationManager.initSynchronization();
        reminders.schedule(aTask().build());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(scheduler.scheduled).isEmpty();
    }

    @Test
    void failuresAreLoggedAtErrorWithTheTaskIdAndExceptionClass() {
        scheduler.failing = true;

        reminders.schedule(aTask().id("task-42").build());
        reminders.cancel("task-43");

        assertThat(logs.list).hasSize(2).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
        assertThat(logs.list.get(0).getFormattedMessage())
                .contains("schedule").contains("task-42").contains("IllegalStateException");
        assertThat(logs.list.get(1).getFormattedMessage())
                .contains("cancel").contains("task-43").contains("IllegalStateException");
    }

    @Test
    void actionsRunOnTheExecutorNotOnTheCallingThread() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>());
        try {
            AtomicReference<Thread> ranOn = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            TaskReminders async = new TaskReminders(new RecordingReminderScheduler() {
                @Override
                public void schedule(com.uniflow.academic.task.domain.Task task) {
                    ranOn.set(Thread.currentThread());
                    done.countDown();
                }
            }, executor);

            async.schedule(aTask().build());

            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(ranOn.get()).isNotSameAs(Thread.currentThread());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aSaturatedExecutorDropsTheReminderAndLogsAnError() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>());
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            TaskReminders saturated = new TaskReminders(scheduler, executor);

            saturated.schedule(aTask().id("dropped-1").build());

            assertThat(logs.list).hasSize(1);
            assertThat(logs.list.get(0).getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.list.get(0).getFormattedMessage()).contains("dropped-1").contains("dropped");
            release.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(scheduler.scheduled).isEmpty();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void asyncActionsStillWaitForTheCommit() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>());
        try {
            CountDownLatch done = new CountDownLatch(1);
            TaskReminders async = new TaskReminders(new RecordingReminderScheduler() {
                @Override
                public void cancel(String taskId) {
                    done.countDown();
                }
            }, executor);
            TransactionSynchronizationManager.initSynchronization();

            async.cancel("t1");
            assertThat(done.await(200, TimeUnit.MILLISECONDS)).isFalse();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
