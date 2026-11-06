package com.uniflow.academic.task.application.services;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.in.ListTasksQuery;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListTasksService implements ListTasksQuery {

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Override
    public PagedResult<Task> execute(String userId, TaskFilter filter, PaginationParams pagination) {
        return taskRepository.search(userId, filter, pagination, clock.instant());
    }
}
