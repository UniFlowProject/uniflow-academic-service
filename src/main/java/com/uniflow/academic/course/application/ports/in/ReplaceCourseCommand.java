package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;

import java.util.List;

public interface ReplaceCourseCommand {

    Course execute(String courseId, ReplaceCourseRequest request, String userId);

    record ReplaceCourseRequest(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description,
            List<String> schedule
    ) {
        public ReplaceCourseRequest {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Course name is required");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Course code is required");
            }
        }
    }
}
