package com.uniflow.academic.task.application.ports.out;

import com.uniflow.academic.task.domain.Task;

/**
 * Schedules a one-time reminder before a task is due. Both calls are idempotent: scheduling again
 * replaces the previous reminder and cancelling a missing one does nothing.
 */
public interface ReminderScheduler {

    void schedule(Task task);

    void cancel(String taskId);
}
