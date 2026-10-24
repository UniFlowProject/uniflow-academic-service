package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;

import java.util.List;

public interface GetCoursesByPeriodQuery {

    List<Course> execute(String periodId, String userId);
}
