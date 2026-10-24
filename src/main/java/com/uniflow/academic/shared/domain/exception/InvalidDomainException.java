package com.uniflow.academic.shared.domain.exception;

/** Input or state violates a business rule (HTTP 400) */
public abstract class InvalidDomainException extends DomainException {
    protected InvalidDomainException(String code, String message) {
        super(code, message);
    }

    protected InvalidDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
