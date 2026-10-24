package com.uniflow.academic.period.domain.exception;

import com.uniflow.academic.shared.domain.exception.InvalidDomainException;

public class InvalidPeriodException extends InvalidDomainException {
    public InvalidPeriodException(String message) {
        super("INVALID_PERIOD", message);
    }

    public InvalidPeriodException(String message, Throwable cause) {
        super("INVALID_PERIOD", message, cause);
    }
}
