package com.uniflow.academic.task.application.services;

import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.task.domain.exception.InvalidTaskException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks that the period and course of a task belong to the user and to each other, and returns
 * their canonical ids.
 */
@Component
@RequiredArgsConstructor
public class TaskParentsResolver {

    private final PeriodRepository periodRepository;
    private final CourseRepository courseRepository;

    public record Parents(String periodId, String courseId) {
    }

    public Parents resolve(String periodId, String courseId, String userId) {
        String resolvedPeriodId = periodRepository.requireOwned(periodId, userId).getId();
        Course course = courseRepository.requireOwned(courseId == null ? "" : courseId.trim(), userId);
        if (!resolvedPeriodId.equals(course.getPeriodId())) {
            throw new InvalidTaskException("Course does not belong to the given period");
        }
        return new Parents(resolvedPeriodId, course.getId());
    }
}
