package com.uniflow.academic.course.domain.exception;

import com.uniflow.academic.shared.domain.exception.ConflictException;

public class CourseDeletionException extends ConflictException {
    public CourseDeletionException(String message) {
        super("COURSE_HAS_TASKS", message);
    }
}
