package com.uniflow.academic.period.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;

public class PeriodDeletionException extends ConflictException {
    public PeriodDeletionException(String message) {
        super("PERIOD_HAS_COURSES", message);
    }
}
