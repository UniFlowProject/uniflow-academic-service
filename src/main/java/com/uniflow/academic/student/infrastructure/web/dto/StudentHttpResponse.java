package com.uniflow.academic.student.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StudentHttpResponse {

    @Schema(description = "Student identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String id;

    @Schema(description = "Student full name", example = "Juan Pérez Rodríguez")
    String name;

    @Schema(description = "Institutional email", example = "juan.perez@estudiantes.tec.ac.cr")
    String email;

    @Schema(description = "Authentication provider", example = "google")
    String provider;

    @Schema(description = "Provider identifier", example = "google_123456789")
    String providerId;

    @Schema(description = "University student ID", example = "2021123456")
    String studentId;

    @Schema(description = "Avatar image URL")
    String avatar;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}
