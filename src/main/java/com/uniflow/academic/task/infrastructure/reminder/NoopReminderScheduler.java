package com.uniflow.academic.task.infrastructure.reminder;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;

/**
 * Default scheduler: reminders are disabled.
 */
public class NoopReminderScheduler implements ReminderScheduler {

    @Override
    public void schedule(Task task) {
    }

    @Override
    public void cancel(String taskId) {
    }
}
