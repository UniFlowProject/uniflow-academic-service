package com.uniflow.academic.course.application.services;

import com.uniflow.academic.course.application.ports.in.GetCoursesQuery;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCoursesService implements GetCoursesQuery {

    private final CourseRepository courseRepository;
    private final PeriodRepository periodRepository;

    @Override
    public List<Course> execute(String userId, CourseFilter filter) {
        log.info("Fetching courses for user {}", userId);
        String periodId = filter != null ? filter.getPeriodId() : null;
        if (periodId != null && !periodId.isBlank()
                && !periodRepository.existsByIdAndUserId(periodId.trim(), userId)) {
            throw new PeriodNotFoundException("Period not found: " + periodId);
        }
        return courseRepository.findAll(userId, filter);
    }
}
