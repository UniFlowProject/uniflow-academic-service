package com.uniflow.academic.subject.infrastructure.web;

import com.uniflow.academic.subject.application.ports.in.*;
import com.uniflow.academic.subject.application.ports.out.dto.SubjectFilter;
import com.uniflow.academic.subject.infrastructure.web.dto.*;
import com.uniflow.academic.subject.infrastructure.web.dto.mapper.SubjectHttpMapper;
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
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class SubjectController {

    private final CreateSubjectCommand createSubjectCommand;
    private final ReplaceSubjectCommand replaceSubjectCommand;
    private final UpdateSubjectCommand updateSubjectCommand;
    private final DeleteSubjectCommand deleteSubjectCommand;
    private final GetSubjectsQuery getSubjectsQuery;
    private final GetSubjectByIdQuery getSubjectByIdQuery;
    private final ValidateSubjectCodeQuery validateSubjectCodeQuery;
    private final SubjectHttpMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new subject")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Subject created successfully",
                    content = @Content(schema = @Schema(implementation = SubjectHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid subject data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SubjectHttpResponse> createSubject(
            @Valid @RequestBody CreateSubjectHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /subjects - create subject");
        String studentId = authentication.getName();
        var commandRequest = mapper.toCreateCommandRequest(request);
        var subject = createSubjectCommand.execute(commandRequest, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toHttpResponse(subject));
    }

    @GetMapping
    @Operation(summary = "Get subjects for the authenticated user")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subjects retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserSubjectsHttpResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserSubjectsHttpResponse> getSubjects(
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) String professor,
            @RequestParam(required = false) Integer credits,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        log.info("GET /subjects - list subjects");
        String studentId = authentication.getName();

        SubjectFilter filter = SubjectFilter.builder()
                .periodId(periodId)
                .professor(professor)
                .credits(credits)
                .search(search)
                .build();

        var subjects = getSubjectsQuery.execute(studentId, filter);
        return ResponseEntity.ok(mapper.toUserSubjectsResponse(subjects));
    }

    @GetMapping("/{subjectId}")
    @Operation(summary = "Get subject by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subject retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SubjectHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Subject not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SubjectHttpResponse> getSubjectById(
            @PathVariable String subjectId,
            Authentication authentication
    ) {
        log.info("GET /subjects/{} - get subject", subjectId);
        String studentId = authentication.getName();
        var subject = getSubjectByIdQuery.execute(subjectId, studentId);
        return ResponseEntity.ok(mapper.toHttpResponse(subject));
    }

    @PutMapping("/{subjectId}")
    @Operation(summary = "Replace subject information")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subject replaced successfully",
                    content = @Content(schema = @Schema(implementation = SubjectHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid subject data"),
            @ApiResponse(responseCode = "404", description = "Subject not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SubjectHttpResponse> replaceSubject(
            @PathVariable String subjectId,
            @Valid @RequestBody ReplaceSubjectHttpRequest request,
            Authentication authentication
    ) {
        log.info("PUT /subjects/{} - update subject", subjectId);
        String studentId = authentication.getName();
        var commandRequest = mapper.toReplaceCommandRequest(request);
        var subject = replaceSubjectCommand.execute(subjectId, commandRequest, studentId);
        return ResponseEntity.ok(mapper.toHttpResponse(subject));
    }

    @PatchMapping("/{subjectId}")
    @Operation(summary = "Update subject information")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subject updated successfully",
                    content = @Content(schema = @Schema(implementation = SubjectHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid subject data"),
            @ApiResponse(responseCode = "404", description = "Subject not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SubjectHttpResponse> updateSubject(
            @PathVariable String subjectId,
            @Valid @RequestBody UpdateSubjectHttpRequest request,
            Authentication authentication
    ) {
        log.info("PATCH /subjects/{} - update subject", subjectId);
        String studentId = authentication.getName();
        var commandRequest = mapper.toUpdateCommandRequest(request);
        var subject = updateSubjectCommand.execute(subjectId, commandRequest, studentId);
        return ResponseEntity.ok(mapper.toHttpResponse(subject));
    }

    @DeleteMapping("/{subjectId}")
    @Operation(summary = "Delete subject")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subject deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Subject not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete subject with associated tasks"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteSubject(
            @PathVariable String subjectId,
            Authentication authentication
    ) {
        log.info("DELETE /subjects/{} - delete subject", subjectId);
        String studentId = authentication.getName();
        deleteSubjectCommand.execute(subjectId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-code")
    @Operation(summary = "Validate subject code availability")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation result",
                    content = @Content(schema = @Schema(implementation = ValidateSubjectCodeHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid validation request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ValidateSubjectCodeHttpResponse> validateCode(
            @Valid @RequestBody ValidateSubjectCodeHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /subjects/validate-code - validate subject code");
        String studentId = authentication.getName();
        var response = validateSubjectCodeQuery.execute(
                request.getCode(),
                request.getPeriodId(),
                studentId,
                request.getSubjectId()
        );
        return ResponseEntity.ok(mapper.toValidateCodeResponse(response));
    }
}
