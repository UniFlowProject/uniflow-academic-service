package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;

public interface GetCourseStatisticsQuery {

    CourseStatisticsResponse execute(String userId);
}
