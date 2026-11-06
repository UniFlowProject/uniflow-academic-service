package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;

import java.util.List;

public interface CreateCourseCommand {

    Course execute(CreateCourseRequest request, String userId);

    record CreateCourseRequest(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String periodId,
            String description,
            List<String> schedule
    ) {
        public CreateCourseRequest {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Course name is required");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Course code is required");
            }
            if (periodId == null || periodId.isBlank()) {
                throw new IllegalArgumentException("Period is required");
            }
        }
    }
}
