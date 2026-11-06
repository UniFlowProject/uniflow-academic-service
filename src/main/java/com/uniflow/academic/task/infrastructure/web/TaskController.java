package com.uniflow.academic.task.infrastructure.web;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.in.ChangeTaskStatusCommand;
import com.uniflow.academic.task.application.ports.in.CreateTaskCommand;
import com.uniflow.academic.task.application.ports.in.DeleteTaskCommand;
import com.uniflow.academic.task.application.ports.in.GetTaskByIdQuery;
import com.uniflow.academic.task.application.ports.in.GetTaskStatsQuery;
import com.uniflow.academic.task.application.ports.in.ListTasksQuery;
import com.uniflow.academic.task.application.ports.in.UpdateTaskCommand;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.infrastructure.web.dto.ChangeTaskStatusHttpRequest;
import com.uniflow.academic.task.infrastructure.web.dto.CreateTaskHttpRequest;
import com.uniflow.academic.task.infrastructure.web.dto.TaskHttpResponse;
import com.uniflow.academic.task.infrastructure.web.dto.TaskStatsHttpResponse;
import com.uniflow.academic.task.infrastructure.web.dto.UpdateTaskHttpRequest;
import com.uniflow.academic.task.infrastructure.web.dto.mapper.TaskHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearer-jwt")
@Validated
public class TaskController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_ID = 36;
    private static final int MAX_SEARCH = 100;

    private final CreateTaskCommand createTaskCommand;
    private final UpdateTaskCommand updateTaskCommand;
    private final ChangeTaskStatusCommand changeTaskStatusCommand;
    private final DeleteTaskCommand deleteTaskCommand;
    private final GetTaskByIdQuery getTaskByIdQuery;
    private final ListTasksQuery listTasksQuery;
    private final GetTaskStatsQuery getTaskStatsQuery;
    private final TaskHttpMapper mapper;

    @PostMapping
    @Operation(summary = "Create a task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(schema = @Schema(implementation = TaskHttpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid task data"),
            @ApiResponse(responseCode = "404", description = "Period or course not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskHttpResponse> createTask(
            @Valid @RequestBody CreateTaskHttpRequest request,
            Authentication authentication
    ) {
        log.info("POST /tasks - create task");
        var task = createTaskCommand.execute(mapper.toDraft(request), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toHttpResponse(task));
    }

    @GetMapping
    @Operation(summary = "List the tasks of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PagedResult<TaskHttpResponse>> listTasks(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> priority,
            @RequestParam(required = false) @Size(max = MAX_ID) String courseId,
            @RequestParam(required = false) @Size(max = MAX_ID) String periodId,
            @RequestParam(required = false) Instant dueDateFrom,
            @RequestParam(required = false) Instant dueDateTo,
            @RequestParam(name = "isOverdue", required = false) Boolean overdue,
            @RequestParam(name = "isDueSoon", required = false) Boolean dueSoon,
            @RequestParam(required = false) @Size(max = MAX_SEARCH) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        log.info("GET /tasks - list tasks");
        var filter = mapper.toFilter(status, priority, courseId, periodId, dueDateFrom, dueDateTo,
                overdue, dueSoon, search, sortBy, sortOrder);
        var tasks = listTasksQuery.execute(
                authentication.getName(), filter, PaginationParams.of(page, limit, DEFAULT_LIMIT));
        return ResponseEntity.ok(mapper.toPagedResponse(tasks));
    }

    // Declared before "/{taskId}" so "stats" is not captured as a task id.
    @GetMapping("/stats")
    @Operation(summary = "Task statistics of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved",
                    content = @Content(schema = @Schema(implementation = TaskStatsHttpResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskStatsHttpResponse> getStats(Authentication authentication) {
        log.info("GET /tasks/stats - task statistics");
        return ResponseEntity.ok(mapper.toStatsResponse(getTaskStatsQuery.execute(authentication.getName())));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task by identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task retrieved",
                    content = @Content(schema = @Schema(implementation = TaskHttpResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskHttpResponse> getTask(@PathVariable @Size(max = MAX_ID) String taskId, Authentication authentication) {
        log.info("GET /tasks/{} - get task", taskId);
        return ResponseEntity.ok(mapper.toHttpResponse(getTaskByIdQuery.execute(taskId, authentication.getName())));
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Update a task partially",
            description = "Only the fields present in the body are changed; actualTimeHours may be set to null to clear it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(schema = @Schema(implementation = TaskHttpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid task data"),
            @ApiResponse(responseCode = "404", description = "Task, period or course not found"),
            @ApiResponse(responseCode = "409", description = "Task is done or cancelled and cannot be modified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskHttpResponse> updateTask(
            @PathVariable @Size(max = MAX_ID) String taskId,
            @Valid @RequestBody UpdateTaskHttpRequest request,
            Authentication authentication
    ) {
        log.info("PATCH /tasks/{} - update task", taskId);
        var task = updateTaskCommand.execute(taskId, mapper.toChanges(request), authentication.getName());
        return ResponseEntity.ok(mapper.toHttpResponse(task));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Change the status of a task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed",
                    content = @Content(schema = @Schema(implementation = TaskHttpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Transition not allowed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskHttpResponse> changeStatus(
            @PathVariable @Size(max = MAX_ID) String taskId,
            @Valid @RequestBody ChangeTaskStatusHttpRequest request,
            Authentication authentication
    ) {
        log.info("PATCH /tasks/{}/status - change status", taskId);
        var task = changeTaskStatusCommand.execute(
                taskId, TaskStatus.fromValue(request.status()), authentication.getName());
        return ResponseEntity.ok(mapper.toHttpResponse(task));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteTask(@PathVariable @Size(max = MAX_ID) String taskId, Authentication authentication) {
        log.info("DELETE /tasks/{} - delete task", taskId);
        deleteTaskCommand.execute(taskId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
