package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ReplaceCourseHttpRequest {

    @Schema(description = "Course name", example = "Diseño de Software")
    @NotBlank
    String name;

    @Schema(description = "Course code", example = "IC-5401")
    @NotBlank
    String code;

    @Schema(description = "Professor name", example = "Marcos Rodríguez")
    String professor;

    @Schema(description = "Course credits", example = "3")
    @NotNull
    @PositiveOrZero
    Integer credits;

    @Schema(description = "Color in HEX", example = "#3b82f6")
    String color;

    @Schema(description = "Course description")
    String description;

    @Schema(description = "Schedule entries", example = "['Lunes 8:00-9:50']")
    @Size(max = 10)
    List<String> schedule;
}
