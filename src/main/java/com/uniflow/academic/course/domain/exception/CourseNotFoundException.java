package com.uniflow.academic.course.domain.exception;

import com.uniflow.academic.shared.domain.exception.NotFoundException;

public class CourseNotFoundException extends NotFoundException {
    public CourseNotFoundException(String message) {
        super("COURSE_NOT_FOUND", message);
    }
}
