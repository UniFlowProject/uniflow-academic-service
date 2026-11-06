package com.uniflow.academic.task.domain.exception;

import com.uniflow.academic.shared.domain.exception.NotFoundException;

public class TaskNotFoundException extends NotFoundException {
    public TaskNotFoundException(String taskId) {
        super("TASK_NOT_FOUND", "Task '" + taskId + "' not found");
    }
}
