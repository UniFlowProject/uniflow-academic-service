package com.uniflow.academic.course.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;

public class CourseCodeAlreadyExistsException extends ConflictException {
    public CourseCodeAlreadyExistsException(String message) {
        super("COURSE_CODE_ALREADY_EXISTS", message);
    }
}
