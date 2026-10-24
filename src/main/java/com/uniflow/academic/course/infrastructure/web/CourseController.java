package com.uniflow.academic.course.infrastructure.web;

import com.uniflow.academic.course.application.ports.in.*;
import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;
import com.uniflow.academic.course.infrastructure.web.dto.*;
import com.uniflow.academic.course.infrastructure.web.dto.mapper.CourseHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
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

@Slf4j
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CourseController {

    private final CreateCourseCommand createCourseCommand;
    private final ReplaceCourseCommand replaceCourseCommand;
    private final UpdateCourseCommand updateCourseCommand;
    private final DeleteCourseCommand deleteCourseCommand;
    private final GetCoursesQuery getCoursesQuery;
    private final GetCourseByIdQuery getCourseByIdQuery;
    private final ValidateCourseCodeQuery validateCourseCodeQuery;
    private final CourseHttpMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new course")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Course created successfully",
                    content = @Content(schema = @Schema(implementation = CourseHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid course data"),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseHttpResponse> createCourse(
            @Valid @RequestBody CreateCourseHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /courses - create course");
        String userId = authentication.getName();
        var commandRequest = mapper.toCreateCommandRequest(request);
        var course = createCourseCommand.execute(commandRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toHttpResponse(course));
    }

    @GetMapping
    @Operation(summary = "Get courses for the authenticated user")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Courses retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserCoursesHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Period filter does not match a period of the user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserCoursesHttpResponse> getCourses(
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) String professor,
            @RequestParam(required = false) Integer credits,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        log.info("GET /courses - list courses");
        String userId = authentication.getName();

        CourseFilter filter = CourseFilter.builder()
                .periodId(periodId)
                .professor(professor)
                .credits(credits)
                .search(search)
                .build();

        var courses = getCoursesQuery.execute(userId, filter);
        return ResponseEntity.ok(mapper.toUserCoursesResponse(courses));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Course retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CourseHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseHttpResponse> getCourseById(
            @PathVariable String courseId,
            Authentication authentication
    ) {
        log.info("GET /courses/{} - get course", courseId);
        String userId = authentication.getName();
        var course = getCourseByIdQuery.execute(courseId, userId);
        return ResponseEntity.ok(mapper.toHttpResponse(course));
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Replace course information")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Course replaced successfully",
                    content = @Content(schema = @Schema(implementation = CourseHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid course data"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseHttpResponse> replaceCourse(
            @PathVariable String courseId,
            @Valid @RequestBody ReplaceCourseHttpRequest request,
            Authentication authentication
    ) {
        log.info("PUT /courses/{} - update course", courseId);
        String userId = authentication.getName();
        var commandRequest = mapper.toReplaceCommandRequest(request);
        var course = replaceCourseCommand.execute(courseId, commandRequest, userId);
        return ResponseEntity.ok(mapper.toHttpResponse(course));
    }

    @PatchMapping("/{courseId}")
    @Operation(summary = "Update course information")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Course updated successfully",
                    content = @Content(schema = @Schema(implementation = CourseHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid course data"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CourseHttpResponse> updateCourse(
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseHttpRequest request,
            Authentication authentication
    ) {
        log.info("PATCH /courses/{} - update course", courseId);
        String userId = authentication.getName();
        var commandRequest = mapper.toUpdateCommandRequest(request);
        var course = updateCourseCommand.execute(courseId, commandRequest, userId);
        return ResponseEntity.ok(mapper.toHttpResponse(course));
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete course")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete course with associated tasks"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteCourse(
            @PathVariable String courseId,
            Authentication authentication
    ) {
        log.info("DELETE /courses/{} - delete course", courseId);
        String userId = authentication.getName();
        deleteCourseCommand.execute(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-code")
    @Operation(summary = "Validate course code availability")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation result",
                    content = @Content(schema = @Schema(implementation = ValidateCourseCodeHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid validation request"),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ValidateCourseCodeHttpResponse> validateCode(
            @Valid @RequestBody ValidateCourseCodeHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /courses/validate-code - validate course code");
        String userId = authentication.getName();
        var response = validateCourseCodeQuery.execute(
                request.getCode(),
                request.getPeriodId(),
                userId,
                request.getCourseId()
        );
        return ResponseEntity.ok(mapper.toValidateCodeResponse(response));
    }
}
