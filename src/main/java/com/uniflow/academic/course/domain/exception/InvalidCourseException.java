package com.uniflow.academic.course.domain.exception;

import com.uniflow.academic.shared.domain.exception.InvalidDomainException;

public class InvalidCourseException extends InvalidDomainException {
    public InvalidCourseException(String message) {
        super("INVALID_COURSE", message);
    }

    public InvalidCourseException(String message, Throwable cause) {
        super("INVALID_COURSE", message, cause);
    }
}
