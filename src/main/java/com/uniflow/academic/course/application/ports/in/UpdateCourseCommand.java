package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;

import java.util.List;
import java.util.Optional;

public interface UpdateCourseCommand {

    Course execute(String courseId, UpdateCourseRequest request, String userId);

    record UpdateCourseRequest(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description
    ) {
        public UpdateCourseRequest {

        }
    }
}
