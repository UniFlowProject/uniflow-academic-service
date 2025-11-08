package com.uniflow.academic.student.infrastructure.web;

import com.uniflow.academic.student.application.ports.in.GetStudentByIdQuery;
import com.uniflow.academic.student.application.ports.in.RegisterStudentCommand;
import com.uniflow.academic.student.infrastructure.web.dto.RegisterStudentHttpRequest;
import com.uniflow.academic.student.infrastructure.web.dto.StudentHttpResponse;
import com.uniflow.academic.student.infrastructure.web.dto.mapper.StudentHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student registration and retrieval endpoints")
public class StudentController {

    private final RegisterStudentCommand registerStudentCommand;
    private final GetStudentByIdQuery getStudentByIdQuery;
    private final StudentHttpMapper mapper;

    @PostMapping("/register")
    @Operation(summary = "Register a student using a Google OAuth token")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Student registered successfully",
                    content = @Content(schema = @Schema(implementation = StudentHttpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid Google token")
    })
    public ResponseEntity<StudentHttpResponse> registerStudent(
            @Valid @RequestBody RegisterStudentHttpRequest request
    ) {
        log.info("POST /students/register - register student");
        var commandRequest = mapper.toRegisterCommand(request);
        var student = registerStudentCommand.register(commandRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toHttpResponse(student));
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "Get student information by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Student retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentHttpResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<StudentHttpResponse> getStudentById(
            @PathVariable String studentId
    ) {
        log.info("GET /students/{} - retrieve student", studentId);
        var student = getStudentByIdQuery.getById(studentId);
        return ResponseEntity.ok(mapper.toHttpResponse(student));
    }
}
