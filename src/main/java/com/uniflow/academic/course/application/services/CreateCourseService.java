package com.uniflow.academic.course.application.services;

import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.course.application.ports.in.CreateCourseCommand;
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
public class CreateCourseService implements CreateCourseCommand {

    private final CourseRepository courseRepository;
    private final PeriodRepository periodRepository;

    @Override
    public Course execute(CreateCourseRequest request, String userId) {
        log.info("Creating course {} for user {}", request.name(), userId);

        String periodId = periodRepository.requireOwned(request.periodId(), userId).getId();

        if (courseRepository.existsByCode(request.code(), periodId, userId, null)) {
            throw new CourseCodeAlreadyExistsException(
                    "Course code already exists in this period"
            );
        }

        List<String> schedule = request.schedule() != null
                ? request.schedule()
                : List.of();

        Course course = Course.create(
                request.name(),
                request.code(),
                request.professor(),
                request.credits(),
                request.color(),
                periodId,
                userId,
                request.description(),
                schedule
        );

        Course saved = courseRepository.save(course);
        log.info("Course {} created successfully", saved.getId());
        return saved;
    }
}
