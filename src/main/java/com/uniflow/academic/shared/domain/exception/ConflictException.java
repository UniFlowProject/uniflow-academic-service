package com.uniflow.academic.shared.domain.exception;

/** Operation conflicts with the current state (HTTP 409) */
public abstract class ConflictException extends DomainException {
    protected ConflictException(String code, String message) {
        super(code, message);
    }

    protected ConflictException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
