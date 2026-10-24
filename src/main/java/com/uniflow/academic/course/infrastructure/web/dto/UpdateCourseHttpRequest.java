package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@Builder
public class UpdateCourseHttpRequest {

    @Schema(description = "Course name", example = "Diseño de Software")
    String name;

    @Schema(description = "Course code", example = "IC-5401")
    String code;

    @Schema(description = "Professor name", example = "Marcos Rodríguez")
    String professor;

    @Schema(description = "Course credits", example = "3")
    Integer credits;

    @Schema(description = "Color in HEX", example = "#3b82f6")
    String color;

    @Schema(description = "Course description")
    String description;
}
