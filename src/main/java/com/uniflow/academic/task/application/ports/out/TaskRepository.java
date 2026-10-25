package com.uniflow.academic.task.application.ports.out;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    /** Insert or replace the task. */
    Task save(Task task);

    Optional<Task> findById(String taskId, String userId);

    PagedResult<Task> search(String userId, TaskFilter filter, PaginationParams pagination, Instant now);

    /** Every task of the user, unpaginated. */
    List<Task> findAllByUserId(String userId);

    /**
     * @return true if a row was deleted, false if the user does not own that task
     */
    boolean delete(String taskId, String userId);

    /**
     * Load a task owned by the user; unknown or foreign ids yield {@link TaskNotFoundException}.
     */
    default Task requireOwned(String taskId, String userId) {
        return findById(taskId, userId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
