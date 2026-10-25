package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;

public interface ListTasksQuery {

    PagedResult<Task> execute(String userId, TaskFilter filter, PaginationParams pagination);
}
