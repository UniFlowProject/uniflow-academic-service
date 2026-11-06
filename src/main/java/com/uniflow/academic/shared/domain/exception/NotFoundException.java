package com.uniflow.academic.shared.domain.exception;

/** Resource does not exist or is not owned by the caller (HTTP 404) */
public abstract class NotFoundException extends DomainException {
    protected NotFoundException(String code, String message) {
        super(code, message);
    }

    protected NotFoundException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
