package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.GetCourseByIdQuery;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCourseByIdService implements GetCourseByIdQuery {

    private final CourseRepository courseRepository;

    @Override
    public Course execute(String courseId, String userId) {
        log.info("Fetching course {} for user {}", courseId, userId);
        return courseRepository.requireOwned(courseId, userId);
    }
}
