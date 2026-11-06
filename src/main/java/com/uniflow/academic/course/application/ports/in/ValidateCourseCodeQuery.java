package com.uniflow.academic.course.application.ports.in;

import com.uniflow.academic.course.application.ports.out.dto.ValidateCourseCodeResponse;

public interface ValidateCourseCodeQuery {

    ValidateCourseCodeResponse execute(
            String code,
            String periodId,
            String userId,
            String courseId
    );
}
