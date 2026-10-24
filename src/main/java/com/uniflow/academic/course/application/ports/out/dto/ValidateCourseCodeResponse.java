package com.uniflow.academic.course.application.ports.out.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidateCourseCodeResponse {
    boolean isValid;
    boolean isAvailable;
    String message;
}
