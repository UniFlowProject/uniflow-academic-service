package com.uniflow.academic.task.application;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryTaskRepository implements TaskRepository {

    final Map<String, Task> tasks = new LinkedHashMap<>();
    TaskFilter lastFilter;
    Instant lastNow;

    @Override
    public Task save(Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(String taskId, String userId) {
        return Optional.ofNullable(tasks.get(taskId)).filter(t -> t.getUserId().equals(userId));
    }

    @Override
    public PagedResult<Task> search(String userId, TaskFilter filter, PaginationParams pagination, Instant now) {
        lastFilter = filter;
        lastNow = now;
        List<Task> mine = findAllByUserId(userId);
        int from = Math.min((pagination.page() - 1) * pagination.limit(), mine.size());
        int to = Math.min(from + pagination.limit(), mine.size());
        return PagedResult.of(mine.subList(from, to), mine.size(), pagination.page(), pagination.limit());
    }

    @Override
    public List<Task> findAllByUserId(String userId) {
        return tasks.values().stream().filter(t -> t.getUserId().equals(userId)).toList();
    }

    @Override
    public boolean delete(String taskId, String userId) {
        return findById(taskId, userId).map(t -> tasks.remove(t.getId()) != null).orElse(false);
    }
}
