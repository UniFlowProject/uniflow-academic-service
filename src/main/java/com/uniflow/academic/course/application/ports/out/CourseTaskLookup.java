package com.uniflow.academic.course.application.ports.out;

/**
 * Lets the course module ask whether a course still has tasks, without depending on the task module.
 */
public interface CourseTaskLookup {

    boolean existsByCourseId(String courseId, String userId);
}
