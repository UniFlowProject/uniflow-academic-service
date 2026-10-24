package com.uniflow.academic.course.application.ports.out;

import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;
import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;
import com.uniflow.academic.course.domain.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Course update(Course course);

    Optional<Course> findById(String courseId, String userId);

    List<Course> findAll(String userId, CourseFilter filter);

    List<Course> findByPeriodId(String periodId, String userId);

    boolean existsByCode(String code, String periodId, String userId, String excludeCourseId);

    void delete(String courseId, String userId);

    boolean hasAssociatedTasks(String courseId);

    CourseStatisticsResponse getStatistics(String userId);
}
