package com.uniflow.academic.support;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double wired as the primary {@link ReminderScheduler}: records every call and can be made to fail.
 */
public class RecordingReminderScheduler implements ReminderScheduler {

    private final List<String> scheduled = new CopyOnWriteArrayList<>();
    private final List<String> cancelled = new CopyOnWriteArrayList<>();
    private volatile boolean failing;

    @Override
    public void schedule(Task task) {
        if (failing) {
            throw new IllegalStateException("scheduler down");
        }
        scheduled.add(task.getId());
    }

    @Override
    public void cancel(String taskId) {
        if (failing) {
            throw new IllegalStateException("scheduler down");
        }
        cancelled.add(taskId);
    }

    public List<String> scheduled() {
        return List.copyOf(scheduled);
    }

    public List<String> cancelled() {
        return List.copyOf(cancelled);
    }

    public void failing(boolean failing) {
        this.failing = failing;
    }

    public void reset() {
        scheduled.clear();
        cancelled.clear();
        failing = false;
    }
}
