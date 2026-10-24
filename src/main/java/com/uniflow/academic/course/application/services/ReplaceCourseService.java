package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.ReplaceCourseCommand;
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
public class ReplaceCourseService implements ReplaceCourseCommand {

    private final CourseRepository courseRepository;

    @Override
    public Course execute(String courseId, ReplaceCourseRequest request, String userId) {
        log.info("Updating course {} for user {}", courseId, userId);

        Course current = courseRepository.requireOwned(courseId, userId);

        if (courseRepository.existsByCode(request.code(), current.getPeriodId(), userId, courseId)) {
            throw new CourseCodeAlreadyExistsException("Course code already exists in this period");
        }

        List<String> schedule = request.schedule() != null ? request.schedule() : List.of();

        Course updated = current.update(
                request.name(),
                request.code(),
                request.professor(),
                request.credits(),
                request.color(),
                request.description(),
                schedule
        );

        Course saved = courseRepository.update(updated);
        log.info("Course {} updated successfully", saved.getId());
        return saved;
    }
}
