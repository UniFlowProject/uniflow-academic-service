package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CoursesByPeriodHttpResponse {

    @Schema(description = "Courses within the period")
    List<CourseHttpResponse> items;
}
