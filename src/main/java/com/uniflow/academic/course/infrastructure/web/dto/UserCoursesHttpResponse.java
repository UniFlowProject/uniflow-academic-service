package com.uniflow.academic.course.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserCoursesHttpResponse {

    @Schema(description = "Courses for the authenticated user")
    List<CourseHttpResponse> data;
}
