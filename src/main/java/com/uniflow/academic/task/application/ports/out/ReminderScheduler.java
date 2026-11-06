package com.uniflow.academic.task.application.ports.out;

import com.uniflow.academic.task.domain.Task;

/**
 * Schedules a one-time reminder before a task is due. All calls are idempotent: scheduling again
 * replaces the previous reminder and cancelling a missing one does nothing.
 */
public interface ReminderScheduler {

    /** For a task that has no reminder yet; does nothing if the reminder time has already passed. */
    void schedule(Task task);

    /** For a task that may already have a reminder; replaces it, or removes it if its time has passed. */
    void reschedule(Task task);

    void cancel(String taskId);
}
