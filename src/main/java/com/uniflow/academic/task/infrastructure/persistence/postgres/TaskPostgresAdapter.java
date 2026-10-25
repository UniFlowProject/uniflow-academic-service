package com.uniflow.academic.task.infrastructure.persistence.postgres;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskStats;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPostgresAdapter implements TaskRepository {

    private final TaskJpaRepository jpaRepository;
    private final TaskEntityMapper mapper;

    /**
     * Inserts a task that has no version yet. Otherwise updates the stored row, which must still exist
     * (404 if it was deleted meanwhile) and carry the version the task was loaded with, or the write is
     * rejected as an optimistic locking failure.
     */
    @Override
    @Transactional
    public Task save(Task task) {
        log.debug("Saving task {}", task.getId());
        if (task.getVersion() == null) {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(task)));
        }
        TaskEntity stored = jpaRepository.findByIdAndUserId(task.getId(), task.getUserId())
                .orElseThrow(() -> new TaskNotFoundException(task.getId()));
        if (!task.getVersion().equals(stored.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(TaskEntity.class, task.getId());
        }
        mapper.copyOnto(stored, task);
        return mapper.toDomain(jpaRepository.saveAndFlush(stored));
    }

    @Override
    public Optional<Task> findById(String taskId, String userId) {
        return jpaRepository.findByIdAndUserId(taskId, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Task> search(String userId, TaskFilter filter, PaginationParams pagination, Instant now) {
        TaskFilter normalized = normalize(filter);
        Page<TaskEntity> page = jpaRepository.findAll(
                TaskSpecifications.matching(userId, normalized, now),
                PageRequest.of(pagination.page() - 1, pagination.limit())
        );
        return PagedResult.of(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getTotalElements(),
                pagination.page(),
                pagination.limit()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStats stats(String userId, Instant now) {
        Map<String, Long> byStatus = toMap(jpaRepository.countByStatus(userId));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long completed = byStatus.getOrDefault(TaskStatus.DONE.value(), 0L);
        long closed = completed + byStatus.getOrDefault(TaskStatus.CANCELLED.value(), 0L);
        return new TaskStats(
                total,
                completed,
                total - closed,
                jpaRepository.countOverdue(userId, now),
                byStatus,
                toMap(jpaRepository.countByPriority(userId)),
                toMap(jpaRepository.countByType(userId)),
                toMap(jpaRepository.countByCourse(userId))
        );
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
    }

    @Override
    @Transactional
    public boolean delete(String taskId, String userId) {
        return jpaRepository.deleteByIdAndUserId(taskId, userId) > 0;
    }

    private static TaskFilter normalize(TaskFilter filter) {
        return filter.toBuilder()
                .search(blankToNull(filter.getSearch()))
                .courseId(blankToNull(filter.getCourseId()))
                .periodId(blankToNull(filter.getPeriodId()))
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
