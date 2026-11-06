package com.uniflow.academic.period.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;

public class PeriodActivationConflictException extends ConflictException {
    public PeriodActivationConflictException(String message) {
        super("PERIOD_ACTIVATION_CONFLICT", message);
    }
}
