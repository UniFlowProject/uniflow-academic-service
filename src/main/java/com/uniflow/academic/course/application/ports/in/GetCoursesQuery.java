package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;

import java.util.List;

public interface GetCoursesQuery {

    List<Course> execute(String userId, CourseFilter filter);
}
