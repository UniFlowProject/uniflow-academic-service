package com.uniflow.academic.period.domain.exception;

import com.uniflow.academic.shared.domain.exception.NotFoundException;

public class PeriodNotFoundException extends NotFoundException {
    public PeriodNotFoundException(String message) {
        super("PERIOD_NOT_FOUND", message);
    }
}
