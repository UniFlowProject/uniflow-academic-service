package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.GetCourseStatisticsQuery;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCourseStatisticsService implements GetCourseStatisticsQuery {

    private final CourseRepository courseRepository;

    @Override
    public CourseStatisticsResponse execute(String userId) {
        log.info("Fetching course statistics for user {}", userId);
        return courseRepository.getStatistics(userId);
    }
}
