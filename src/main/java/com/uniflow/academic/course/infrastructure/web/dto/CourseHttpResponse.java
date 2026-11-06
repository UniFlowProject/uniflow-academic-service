package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class CourseHttpResponse {

    @Schema(description = "Course identifier", example = "course-ic-6821")
    String id;

    @Schema(description = "Course name")
    String name;

    @Schema(description = "Course code")
    String code;

    @Schema(description = "Professor in charge")
    String professor;

    @Schema(description = "Credits amount")
    Integer credits;

    @Schema(description = "Color in HEX")
    String color;

    @Schema(description = "Associated period identifier")
    String periodId;

    @Schema(description = "Course description")
    String description;

    @Schema(description = "Schedule entries")
    List<String> schedule;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Update timestamp")
    LocalDateTime updatedAt;
}
