package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;

public interface GetCourseByIdQuery {

    Course execute(String courseId, String userId);
}
