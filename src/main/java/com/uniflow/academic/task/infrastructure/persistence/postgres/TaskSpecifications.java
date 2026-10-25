package com.uniflow.academic.task.infrastructure.persistence.postgres;

import com.uniflow.academic.task.application.ports.out.dto.SortOrder;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.application.ports.out.dto.TaskSortField;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.WireEnum;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the filtered and ordered task query. Priority and status are sorted by rank (declaration
 * order of the enums), not alphabetically; createdAt then id break ties so pages are stable.
 */
final class TaskSpecifications {

    private static final Duration DUE_SOON_WINDOW = Duration.ofHours(24);
    private static final char LIKE_ESCAPE = '\\';

    private TaskSpecifications() {
    }

    static Specification<TaskEntity> matching(String userId, TaskFilter filter, Instant now) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(orderBy(root, cb, filter));
            }
            return cb.and(predicates(root, cb, userId, filter, now).toArray(Predicate[]::new));
        };
    }

    private static List<Predicate> predicates(
            Root<TaskEntity> root, CriteriaBuilder cb, String userId, TaskFilter filter, Instant now
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("userId"), userId));
        if (!filter.getStatuses().isEmpty()) {
            predicates.add(root.get("status").in(filter.getStatuses().stream().map(TaskStatus::value).toList()));
        }
        if (!filter.getPriorities().isEmpty()) {
            predicates.add(root.get("priority").in(filter.getPriorities().stream().map(TaskPriority::value).toList()));
        }
        if (filter.getCourseId() != null) {
            predicates.add(cb.equal(root.get("courseId"), filter.getCourseId()));
        }
        if (filter.getPeriodId() != null) {
            predicates.add(cb.equal(root.get("periodId"), filter.getPeriodId()));
        }
        if (filter.getDueDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("dueDate"), filter.getDueDateFrom()));
        }
        if (filter.getDueDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("dueDate"), filter.getDueDateTo()));
        }
        if (filter.getOverdue() != null) {
            predicates.add(expected(cb, overdue(root, cb, now), filter.getOverdue()));
        }
        if (filter.getDueSoon() != null) {
            predicates.add(expected(cb, dueSoon(root, cb, now), filter.getDueSoon()));
        }
        if (filter.getSearch() != null) {
            String pattern = likePattern(filter.getSearch());
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)
            ));
        }
        return predicates;
    }

    private static Predicate modifiable(Root<TaskEntity> root, CriteriaBuilder cb) {
        return cb.not(root.get("status").in(TaskStatus.DONE.value(), TaskStatus.CANCELLED.value()));
    }

    private static Predicate overdue(Root<TaskEntity> root, CriteriaBuilder cb, Instant now) {
        return cb.and(modifiable(root, cb), cb.lessThan(root.<Instant>get("dueDate"), now));
    }

    private static Predicate dueSoon(Root<TaskEntity> root, CriteriaBuilder cb, Instant now) {
        return cb.and(
                modifiable(root, cb),
                cb.greaterThanOrEqualTo(root.<Instant>get("dueDate"), now),
                cb.lessThanOrEqualTo(root.<Instant>get("dueDate"), now.plus(DUE_SOON_WINDOW))
        );
    }

    private static Predicate expected(CriteriaBuilder cb, Predicate predicate, boolean expectation) {
        return expectation ? predicate : cb.not(predicate);
    }

    private static List<Order> orderBy(Root<TaskEntity> root, CriteriaBuilder cb, TaskFilter filter) {
        Expression<?> primary = switch (filter.getSortBy()) {
            case DUE_DATE -> root.get("dueDate");
            case CREATED_AT -> root.get("createdAt");
            case PRIORITY -> rank(root.get("priority"), cb, TaskPriority.values());
            case STATUS -> rank(root.get("status"), cb, TaskStatus.values());
        };
        List<Order> orders = new ArrayList<>();
        orders.add(filter.getSortOrder() == SortOrder.DESC ? cb.desc(primary) : cb.asc(primary));
        if (filter.getSortBy() != TaskSortField.CREATED_AT) {
            orders.add(cb.asc(root.get("createdAt")));
        }
        orders.add(cb.asc(root.get("id")));
        return orders;
    }

    private static <E extends Enum<E> & WireEnum> Expression<Integer> rank(Path<String> column, CriteriaBuilder cb, E[] values) {
        CriteriaBuilder.Case<Integer> rank = cb.selectCase();
        for (E value : values) {
            rank = rank.when(cb.equal(column, value.value()), value.ordinal());
        }
        return rank.otherwise(values.length);
    }

    private static String likePattern(String search) {
        String escaped = search.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
