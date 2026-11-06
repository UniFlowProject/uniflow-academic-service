package com.uniflow.academic.task.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;
import com.uniflow.academic.task.domain.TaskStatus;

public class TaskNotModifiableException extends ConflictException {
    public TaskNotModifiableException(String taskId, TaskStatus status) {
        super("TASK_NOT_MODIFIABLE", "Task '" + taskId + "' is " + status.value() + " and cannot be modified");
    }
}
