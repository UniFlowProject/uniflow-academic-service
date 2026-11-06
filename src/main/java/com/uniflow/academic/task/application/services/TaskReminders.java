package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Best-effort reminder changes. Inside a transaction they are submitted only after it commits, so a
 * rolled back write never leaves a schedule, and they run on a separate executor so neither the
 * database connection nor the request thread waits for AWS. A failure, or a saturated executor, is
 * logged at ERROR and never fails the operation.
 */
@Slf4j
@Component
public class TaskReminders {

    private final ReminderScheduler scheduler;
    private final Executor executor;

    public TaskReminders(ReminderScheduler scheduler, @Qualifier("reminderExecutor") Executor executor) {
        this.scheduler = scheduler;
        this.executor = executor;
    }

    public void schedule(Task task) {
        afterCommit("schedule", task.getId(), () -> scheduler.schedule(task));
    }

    public void reschedule(Task task) {
        afterCommit("reschedule", task.getId(), () -> scheduler.reschedule(task));
    }

    public void cancel(String taskId) {
        afterCommit("cancel", taskId, () -> scheduler.cancel(taskId));
    }

    private void afterCommit(String operation, String taskId, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submit(operation, taskId, action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submit(operation, taskId, action);
            }
        });
    }

    private void submit(String operation, String taskId, Runnable action) {
        try {
            executor.execute(() -> runQuietly(operation, taskId, action));
        } catch (RejectedExecutionException e) {
            log.error("Reminder {} for task {} dropped: the reminder executor is saturated", operation, taskId);
        }
    }

    private void runQuietly(String operation, String taskId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Could not {} reminder for task {}: {}", operation, taskId, e.getClass().getName(), e);
        }
    }
}
