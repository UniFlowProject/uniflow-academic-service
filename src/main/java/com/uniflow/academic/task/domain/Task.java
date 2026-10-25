package com.uniflow.academic.task.domain;

import com.uniflow.academic.task.domain.exception.InvalidStatusTransitionException;
import com.uniflow.academic.task.domain.exception.InvalidTaskException;
import com.uniflow.academic.task.domain.exception.TaskNotModifiableException;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable task aggregate. Every state change returns a new, validated instance.
 */
@Value
@Builder(toBuilder = true)
public class Task {

    private static final Duration DUE_SOON_WINDOW = Duration.ofHours(24);

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.TODO, TaskStatus.IN_REVIEW, TaskStatus.DONE, TaskStatus.CANCELLED),
            TaskStatus.IN_REVIEW, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED),
            TaskStatus.DONE, Set.of(),
            TaskStatus.CANCELLED, Set.of()
    );

    String id;
    String userId;
    String title;
    String description;
    String courseId;
    String periodId;
    Instant dueDate;
    TaskStatus status;
    TaskPriority priority;
    TaskType type;
    Integer estimatedTimeHours;
    Integer actualTimeHours;
    List<String> tags;
    List<String> groupMembers;
    List<String> attachments;
    Instant createdAt;
    Instant updatedAt;
    Instant completedAt;

    public static Task create(String id, String userId, TaskDraft draft, Instant now) {
        Task task = Task.builder()
                .id(id)
                .userId(userId)
                .title(draft.title())
                .description(draft.description())
                .courseId(draft.courseId())
                .periodId(draft.periodId())
                .dueDate(draft.dueDate())
                .status(TaskStatus.TODO)
                .priority(draft.priority())
                .type(draft.type())
                .estimatedTimeHours(draft.estimatedTimeHours())
                .tags(draft.tags())
                .groupMembers(draft.groupMembers())
                .attachments(draft.attachments())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return task.normalized().validated();
    }

    public static Set<TaskStatus> allowedTransitions(TaskStatus from) {
        return ALLOWED_TRANSITIONS.get(from);
    }

    public boolean isGroupWork() {
        return type == TaskType.GROUP_WORK;
    }

    public boolean canBeModified() {
        return status != TaskStatus.DONE && status != TaskStatus.CANCELLED;
    }

    public boolean canBeDeleted() {
        return canBeModified();
    }

    public boolean isOverdue(Instant now) {
        return canBeModified() && dueDate.isBefore(now);
    }

    public boolean isDueSoon(Instant now) {
        return canBeModified() && !dueDate.isBefore(now) && !dueDate.isAfter(now.plus(DUE_SOON_WINDOW));
    }

    public void ensureModifiable() {
        if (!canBeModified()) {
            throw new TaskNotModifiableException(id, status);
        }
    }

    public Task withChanges(TaskChanges changes, Instant now) {
        ensureModifiable();
        TaskBuilder builder = toBuilder().updatedAt(now);
        if (changes.title() != null) builder.title(changes.title());
        if (changes.description() != null) builder.description(changes.description());
        if (changes.courseId() != null) builder.courseId(changes.courseId());
        if (changes.periodId() != null) builder.periodId(changes.periodId());
        if (changes.dueDate() != null) builder.dueDate(changes.dueDate());
        if (changes.priority() != null) builder.priority(changes.priority());
        if (changes.type() != null) builder.type(changes.type());
        if (changes.estimatedTimeHours() != null) builder.estimatedTimeHours(changes.estimatedTimeHours());
        if (changes.clearActualTimeHours()) builder.actualTimeHours(null);
        else if (changes.actualTimeHours() != null) builder.actualTimeHours(changes.actualTimeHours());
        if (changes.tags() != null) builder.tags(changes.tags());
        if (changes.groupMembers() != null) builder.groupMembers(changes.groupMembers());
        if (changes.attachments() != null) builder.attachments(changes.attachments());
        return builder.build().normalized().validated();
    }

    public Task transitionTo(TaskStatus newStatus, Instant now) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(newStatus)) {
            throw new InvalidStatusTransitionException(status, newStatus);
        }
        return toBuilder()
                .status(newStatus)
                .updatedAt(now)
                .completedAt(newStatus == TaskStatus.DONE ? now : completedAt)
                .build();
    }

    private Task normalized() {
        return toBuilder()
                .description(description == null ? "" : description)
                .tags(tags == null ? List.of() : List.copyOf(tags))
                .groupMembers(groupMembers == null ? List.of() : List.copyOf(groupMembers))
                .attachments(attachments == null ? List.of() : List.copyOf(attachments))
                .build();
    }

    private Task validated() {
        if (isBlank(id)) throw new InvalidTaskException("Task id is required");
        if (isBlank(userId)) throw new InvalidTaskException("Task user is required");
        if (isBlank(title)) throw new InvalidTaskException("Task title is required");
        if (isBlank(courseId)) throw new InvalidTaskException("Task course is required");
        if (isBlank(periodId)) throw new InvalidTaskException("Task period is required");
        if (dueDate == null) throw new InvalidTaskException("Task due date is required");
        if (priority == null) throw new InvalidTaskException("Task priority is required");
        if (type == null) throw new InvalidTaskException("Task type is required");
        if (estimatedTimeHours == null || estimatedTimeHours < 0) {
            throw new InvalidTaskException("Estimated time hours must be zero or positive");
        }
        if (actualTimeHours != null && actualTimeHours < 0) {
            throw new InvalidTaskException("Actual time hours must be zero or positive");
        }
        if (!groupMembers.isEmpty() && !isGroupWork()) {
            throw new InvalidTaskException("Group members require a task of type 'group-work'");
        }
        return this;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
