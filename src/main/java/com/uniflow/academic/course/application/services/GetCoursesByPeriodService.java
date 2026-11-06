package com.uniflow.academic.course.application.services;

import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.course.application.ports.in.GetCoursesByPeriodQuery;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCoursesByPeriodService implements GetCoursesByPeriodQuery {

    private final CourseRepository courseRepository;
    private final PeriodRepository periodRepository;

    @Override
    public List<Course> execute(String periodId, String userId) {
        log.info("Fetching courses for period {} and user {}", periodId, userId);

        String ownedPeriodId = periodRepository.requireOwned(periodId, userId).getId();

        return courseRepository.findByPeriodId(ownedPeriodId, userId);
    }
}
