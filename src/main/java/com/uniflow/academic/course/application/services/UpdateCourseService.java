package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.ReplaceCourseCommand;
import com.uniflow.academic.course.application.ports.in.UpdateCourseCommand;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.course.domain.exception.CourseCodeAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateCourseService implements UpdateCourseCommand {

    private final CourseRepository courseRepository;

    @Override
    public Course execute(String courseId, UpdateCourseRequest request, String userId) {
        log.info("Updating course {} for user {}", courseId, userId);

        Course current = courseRepository.requireOwned(courseId, userId);

        if (request.code() != null) {
            if(courseRepository.existsByCode(request.code(), current.getPeriodId(), userId, courseId)) {
                throw new CourseCodeAlreadyExistsException("Course code already exists in this period");
            }
        }

        Course updated = current.patch(
                request.name(),
                request.code(),
                request.professor(),
                request.credits(),
                request.color(),
                request.description()
        );

        Course saved = courseRepository.update(updated);
        log.info("Course {} updated successfully", saved.getId());
        return saved;
    }
}
