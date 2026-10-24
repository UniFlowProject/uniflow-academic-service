package com.uniflow.academic.course.infrastructure.web;

import com.uniflow.academic.course.application.ports.in.GetCourseStatisticsQuery;
import com.uniflow.academic.course.infrastructure.web.dto.CourseStatisticsHttpResponse;
import com.uniflow.academic.course.infrastructure.web.dto.mapper.CourseHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CourseStatisticsController {

    private final GetCourseStatisticsQuery getCourseStatisticsQuery;
    private final CourseHttpMapper mapper;

    @GetMapping("/courses")
    @Operation(summary = "Get course statistics")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CourseStatisticsHttpResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseStatisticsHttpResponse> getCourseStatistics(Authentication authentication) {
        log.info("GET /stats/courses - course statistics");
        String userId = authentication.getName();
        var response = getCourseStatisticsQuery.execute(userId);
        return ResponseEntity.ok(mapper.toStatisticsHttpResponse(response));
    }
}
