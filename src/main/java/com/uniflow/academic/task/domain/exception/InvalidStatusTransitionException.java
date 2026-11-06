package com.uniflow.academic.task.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;
import com.uniflow.academic.task.domain.TaskStatus;

public class InvalidStatusTransitionException extends ConflictException {
    public InvalidStatusTransitionException(TaskStatus current, TaskStatus target) {
        super("INVALID_STATUS_TRANSITION",
                "Cannot move task from '" + current.value() + "' to '" + target.value() + "'");
    }
}
