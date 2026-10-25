package com.uniflow.academic.task.infrastructure.persistence.postgres;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.domain.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPostgresAdapter implements TaskRepository {

    private final TaskJpaRepository jpaRepository;
    private final TaskEntityMapper mapper;

    @Override
    public Task save(Task task) {
        log.debug("Saving task {}", task.getId());
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(task)));
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
    public List<Task> findAllByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean delete(String taskId, String userId) {
        return jpaRepository.deleteByIdAndUserId(taskId, userId) > 0;
    }

    @Override
    public boolean existsByCourseId(String courseId, String userId) {
        return jpaRepository.existsByCourseIdAndUserId(courseId, userId);
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
