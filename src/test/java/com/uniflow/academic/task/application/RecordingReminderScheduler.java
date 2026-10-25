package com.uniflow.academic.task.application;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;

import java.util.ArrayList;
import java.util.List;

class RecordingReminderScheduler implements ReminderScheduler {

    final List<Task> scheduled = new ArrayList<>();
    final List<String> cancelled = new ArrayList<>();
    boolean failing;

    @Override
    public void schedule(Task task) {
        if (failing) {
            throw new IllegalStateException("scheduler down");
        }
        scheduled.add(task);
    }

    @Override
    public void cancel(String taskId) {
        if (failing) {
            throw new IllegalStateException("scheduler down");
        }
        cancelled.add(taskId);
    }
}
