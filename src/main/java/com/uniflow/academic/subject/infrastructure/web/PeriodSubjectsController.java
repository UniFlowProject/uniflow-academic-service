package com.uniflow.academic.subject.infrastructure.web;

import com.uniflow.academic.subject.application.ports.in.GetSubjectsByPeriodQuery;
import com.uniflow.academic.subject.infrastructure.web.dto.SubjectsByPeriodHttpResponse;
import com.uniflow.academic.subject.infrastructure.web.dto.mapper.SubjectHttpMapper;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/periods/{periodId}/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PeriodSubjectsController {

    private final GetSubjectsByPeriodQuery getSubjectsByPeriodQuery;
    private final SubjectHttpMapper mapper;

    @GetMapping
    @Operation(summary = "Get subjects by period")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subjects retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SubjectsByPeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Period not found")
    })
    public ResponseEntity<SubjectsByPeriodHttpResponse> getSubjectsByPeriod(
            @PathVariable String periodId,
            Authentication authentication
    ) {
        log.info("GET /periods/{}/subjects - list subjects", periodId);
        String studentId = authentication.getName();
        var subjects = getSubjectsByPeriodQuery.execute(periodId, studentId);
        return ResponseEntity.ok(mapper.toSubjectsByPeriodResponse(subjects));
    }
}
