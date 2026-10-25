package com.uniflow.academic.task.application;

import com.uniflow.academic.task.application.services.TaskReminders;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;

class TaskRemindersTest {

    private final RecordingReminderScheduler scheduler = new RecordingReminderScheduler();
    private final TaskReminders reminders = new TaskReminders(scheduler);

    @Test
    void runsImmediatelyWithoutATransaction() {
        reminders.schedule(aTask().build());
        reminders.cancel("t1");

        assertThat(scheduler.scheduled).hasSize(1);
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void defersUntilAfterCommitInsideATransaction() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            reminders.schedule(aTask().build());
            reminders.cancel("t1");

            assertThat(scheduler.scheduled).isEmpty();
            assertThat(scheduler.cancelled).isEmpty();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            assertThat(scheduler.scheduled).hasSize(1);
            assertThat(scheduler.cancelled).containsExactly("t1");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void neverRunsWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            reminders.schedule(aTask().build());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            assertThat(scheduler.scheduled).isEmpty();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void failuresAreSwallowedInBothModes() {
        scheduler.failing = true;
        reminders.schedule(aTask().build());
        reminders.cancel("t1");

        TransactionSynchronizationManager.initSynchronization();
        try {
            reminders.schedule(aTask().build());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(scheduler.scheduled).isEmpty();
    }
}
