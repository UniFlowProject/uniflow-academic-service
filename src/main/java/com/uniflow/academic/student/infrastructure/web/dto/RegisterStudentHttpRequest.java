package com.uniflow.academic.student.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterStudentHttpRequest {

    @Schema(description = "Google OAuth access token", example = "ya29.a0AfH6SMDX...")
    @NotBlank
    String accessToken;
}
