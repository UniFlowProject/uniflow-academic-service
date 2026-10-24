package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidateCourseCodeHttpResponse {

    @Schema(description = "Indicates if the code format is valid")
    boolean isValid;

    @Schema(description = "Indicates if the code is available")
    boolean isAvailable;

    @Schema(description = "Validation message")
    String message;
}
