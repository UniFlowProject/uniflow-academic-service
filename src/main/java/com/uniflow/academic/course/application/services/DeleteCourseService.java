package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.DeleteCourseCommand;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.exception.CourseDeletionException;
import com.uniflow.academic.course.domain.exception.CourseNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteCourseService implements DeleteCourseCommand {

    private final CourseRepository courseRepository;

    @Override
    public void execute(String courseId, String userId) {
        log.info("Deleting course {} for user {}", courseId, userId);

        courseRepository.requireOwned(courseId, userId);

        if (courseRepository.hasAssociatedTasks(courseId)) {
            throw new CourseDeletionException("Cannot delete course with associated tasks");
        }

        if (!courseRepository.delete(courseId, userId)) {
            throw new CourseNotFoundException("Course not found");
        }
        log.info("Course {} deleted", courseId);
    }
}
