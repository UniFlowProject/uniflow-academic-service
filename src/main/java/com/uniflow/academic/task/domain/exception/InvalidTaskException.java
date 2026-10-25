package com.uniflow.academic.task.domain.exception;

import com.uniflow.academic.shared.domain.exception.InvalidDomainException;

public class InvalidTaskException extends InvalidDomainException {
    public InvalidTaskException(String message) {
        super("INVALID_TASK", message);
    }
}
