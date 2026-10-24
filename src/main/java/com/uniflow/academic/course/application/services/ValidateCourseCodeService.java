package com.uniflow.academic.course.application.services;

import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.course.application.ports.in.ValidateCourseCodeQuery;
import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.application.ports.out.dto.ValidateCourseCodeResponse;
import com.uniflow.academic.course.domain.exception.InvalidCourseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidateCourseCodeService implements ValidateCourseCodeQuery {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z]{2,4}-?\\d{3,4}$");

    private final CourseRepository courseRepository;
    private final PeriodRepository periodRepository;

    @Override
    public ValidateCourseCodeResponse execute(
            String code,
            String periodId,
            String userId,
            String courseId
    ) {
        log.info("Validating code {} for period {} and user {}", code, periodId, userId);

        if (code == null || code.isBlank()) {
            throw new InvalidCourseException("Course code is required");
        }
        if (periodId == null || periodId.isBlank()) {
            throw new InvalidCourseException("Period is required");
        }

        periodId = periodRepository.requireOwned(periodId, userId).getId();

        boolean matchesPattern = CODE_PATTERN.matcher(code).matches();
        boolean exists = courseRepository.existsByCode(code, periodId, userId, courseId);

        String message;
        if (!matchesPattern) {
            message = "Course code format is invalid";
        } else if (exists) {
            message = "Course code format is valid but already exists in this period";
        } else {
            message = "Course code is available";
        }

        return ValidateCourseCodeResponse.builder()
                .isValid(matchesPattern)
                .isAvailable(matchesPattern && !exists)
                .message(message)
                .build();
    }
}
