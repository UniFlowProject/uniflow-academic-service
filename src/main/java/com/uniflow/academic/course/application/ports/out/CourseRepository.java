package com.uniflow.academic.course.application.ports.out;

import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;
import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.course.domain.exception.CourseNotFoundException;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Course update(Course course);

    Optional<Course> findById(String courseId, String userId);

    List<Course> findAll(String userId, CourseFilter filter);

    List<Course> findByPeriodId(String periodId, String userId);

    boolean existsByCode(String code, String periodId, String userId, String excludeCourseId);

    /**
     * Delete a course owned by the user.
     *
     * @return true if a row was deleted, false if the user does not own that course
     */
    boolean delete(String courseId, String userId);

    boolean existsByPeriodId(String periodId, String userId);

    boolean hasAssociatedTasks(String courseId);

    CourseStatisticsResponse getStatistics(String userId);

    /**
     * Load a course owned by the user; unknown or foreign ids yield {@link CourseNotFoundException}.
     */
    default Course requireOwned(String courseId, String userId) {
        return findById(courseId, userId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));
    }
}
