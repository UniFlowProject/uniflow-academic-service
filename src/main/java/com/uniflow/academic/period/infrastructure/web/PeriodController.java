package com.uniflow.academic.period.infrastructure.web;

import com.uniflow.academic.period.application.ports.in.*;
import com.uniflow.academic.period.application.ports.out.dto.PeriodFilter;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.period.infrastructure.web.dto.*;
import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.period.infrastructure.web.dto.mapper.PeriodHttpMapper;
import com.uniflow.academic.course.application.ports.in.GetCoursesByPeriodQuery;
import com.uniflow.academic.course.infrastructure.web.dto.CoursesByPeriodHttpResponse;
import com.uniflow.academic.course.infrastructure.web.dto.mapper.CourseHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Period management endpoints.
 * Implements all period-related HTTP operations.
 */
@Slf4j
@RestController
@RequestMapping("/periods")
@RequiredArgsConstructor
@Tag(name = "Periods", description = "Period management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PeriodController {

    private final CreatePeriodCommand createPeriodCommand;
    private final UpdatePeriodCommand updatePeriodCommand;
    private final DeletePeriodCommand deletePeriodCommand;
    private final ActivatePeriodCommand activatePeriodCommand;
    private final GetAllPeriodsQuery getAllPeriodsQuery;
    private final GetPeriodByIdQuery getPeriodByIdQuery;
    private final GetCurrentPeriodQuery getCurrentPeriodQuery;
    private final GetPeriodStatisticsQuery getPeriodStatisticsQuery;
    private final PeriodHttpMapper periodHttpMapper;
    private final GetCoursesByPeriodQuery getCoursesByPeriodQuery;
    private final CourseHttpMapper courseHttpMapper;

    /**
     * POST /periods - Create a new period
     */
    @PostMapping
    @Operation(
            summary = "Create a new period",
            description = "Create a new academic period for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Period created successfully",
                    content = @Content(schema = @Schema(implementation = PeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid period data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodHttpResponse> createPeriod(
            @Valid @RequestBody CreatePeriodHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /periods - Create period");
        String userId = authentication.getName();

        CreatePeriodCommand.CreatePeriodRequest commandRequest =
                periodHttpMapper.toCreatePeriodCommandRequest(request);

        var period = createPeriodCommand.execute(commandRequest, userId);
        PeriodHttpResponse response = periodHttpMapper.toHttpResponse(period);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /periods - Get all periods with optional filters
     */
    @GetMapping
    @Operation(
            summary = "Get all periods",
            description = "Retrieve all periods for the authenticated user with optional filters and pagination"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Periods retrieved successfully"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PagedResult<PeriodHttpResponse>> getAllPeriods(
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "Filter by period type")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by year")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive,

            Authentication authentication
    ) {
        log.info("GET /periods - Get all periods");
        String userId = authentication.getName();

        PaginationParams params = PaginationParams.of(page, limit);

        PeriodFilter filter = PeriodFilter.builder()
                .type(type)
                .year(year)
                .isActive(isActive)
                .build();

        var response = getAllPeriodsQuery.execute(userId, params, filter);
        return ResponseEntity.ok(periodHttpMapper.toPagedResponse(response));
    }

    /**
     * GET /periods/current - Get current active period
     */
    @GetMapping("/current")
    @Operation(
            summary = "Get current active period",
            description = "Retrieve the currently active period for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current period retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "No active period found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodHttpResponse> getCurrentPeriod(
            Authentication authentication
    ) {
        log.info("GET /periods/current - Get current period");
        String userId = authentication.getName();

        var period = getCurrentPeriodQuery.execute(userId);
        return ResponseEntity.ok(periodHttpMapper.toHttpResponse(period));
    }

    /**
     * GET /periods/{periodId} - Get period by ID
     */
    @GetMapping("/{periodId}")
    @Operation(
            summary = "Get period by ID",
            description = "Retrieve a specific period by its ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Period retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodHttpResponse> getPeriodById(
            @Parameter(description = "Period ID")
            @PathVariable String periodId,

            Authentication authentication
    ) {
        log.info("GET /periods/{} - Get period by ID", periodId);
        String userId = authentication.getName();

        var period = getPeriodByIdQuery.execute(periodId, userId);
        return ResponseEntity.ok(periodHttpMapper.toHttpResponse(period));
    }

    /**
     * PUT /periods/{periodId} - Update period
     */
    @PutMapping("/{periodId}")
    @Operation(
            summary = "Update period",
            description = "Update an existing period"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Period updated successfully",
                    content = @Content(schema = @Schema(implementation = PeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid period data"),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodHttpResponse> updatePeriod(
            @Parameter(description = "Period ID")
            @PathVariable String periodId,

            @Valid @RequestBody UpdatePeriodHttpRequest request,
            Authentication authentication
    ) {
        log.info("PUT /periods/{} - Update period", periodId);
        String userId = authentication.getName();

        var commandRequest = periodHttpMapper.toUpdatePeriodCommandRequest(request);

        var period = updatePeriodCommand.execute(
                periodId,
                commandRequest,
                userId
        );

        return ResponseEntity.ok(periodHttpMapper.toHttpResponse(period));
    }

    /**
     * DELETE /periods/{periodId} - Delete period
     */
    @DeleteMapping("/{periodId}")
    @Operation(
            summary = "Delete period",
            description = "Delete a period (only if it has no associated courses)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Period deleted successfully"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot delete period with associated courses"
            ),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deletePeriod(
            @Parameter(description = "Period ID")
            @PathVariable String periodId,

            Authentication authentication
    ) {
        log.info("DELETE /periods/{} - Delete period", periodId);
        String userId = authentication.getName();

        deletePeriodCommand.execute(periodId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /periods/{periodId}/activate - Activate period
     */
    @PatchMapping("/{periodId}/activate")
    @Operation(
            summary = "Activate period",
            description = "Activate a period (deactivates all others for the user)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Period activated successfully",
                    content = @Content(schema = @Schema(implementation = PeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodHttpResponse> activatePeriod(
            @Parameter(description = "Period ID")
            @PathVariable String periodId,

            Authentication authentication
    ) {
        log.info("PATCH /periods/{}/activate - Activate period", periodId);
        String userId = authentication.getName();

        var period = activatePeriodCommand.execute(periodId, userId);
        return ResponseEntity.ok(periodHttpMapper.toHttpResponse(period));
    }

    /**
     * GET /periods/{periodId}/courses - Get period courses
     */
    @GetMapping("/{periodId}/courses")
    @Operation(summary = "Get courses by period")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Courses retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CoursesByPeriodHttpResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Period not found")
    })
    public ResponseEntity<CoursesByPeriodHttpResponse> getCoursesByPeriod(
            @PathVariable String periodId,
            Authentication authentication
    ) {
        log.info("GET /periods/{}/courses - list courses", periodId);
        String userId = authentication.getName();
        var courses = getCoursesByPeriodQuery.execute(periodId, userId);
        return ResponseEntity.ok(courseHttpMapper.toCoursesByPeriodResponse(courses));
    }

    /**
     * GET /stats/periods - Get period statistics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get period statistics",
            description = "Get statistics about the user's periods"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = PeriodStatisticsHttpResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PeriodStatisticsHttpResponse> getPeriodStatistics(
            Authentication authentication
    ) {
        log.info("GET /stats/periods - Get period statistics");
        String userId = authentication.getName();

        var stats = getPeriodStatisticsQuery.execute(userId);
        return ResponseEntity.ok(periodHttpMapper.toStatisticsHttpResponse(stats));
    }
}