package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Best-effort reminder changes. Inside a transaction they run only after it commits, so a rolled
 * back write never leaves a schedule; a scheduler failure is logged and never fails the operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminders {

    private final ReminderScheduler scheduler;

    public void schedule(Task task) {
        afterCommit(() -> runQuietly("schedule", task.getId(), () -> scheduler.schedule(task)));
    }

    public void cancel(String taskId) {
        afterCommit(() -> runQuietly("cancel", taskId, () -> scheduler.cancel(taskId)));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void runQuietly(String operation, String taskId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Could not {} reminder for task {}", operation, taskId, e);
        }
    }
}
