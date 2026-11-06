package com.uniflow.academic.task.domain;

import com.uniflow.academic.task.domain.exception.InvalidStatusTransitionException;
import com.uniflow.academic.task.domain.exception.InvalidTaskException;
import com.uniflow.academic.task.domain.exception.TaskNotModifiableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    private static final Instant LATER = NOW.plus(Duration.ofHours(1));

    private static TaskDraft draft(TaskType type, List<String> groupMembers) {
        return new TaskDraft("Essay", null, "c1", "p1", NOW.plus(Duration.ofDays(2)),
                TaskPriority.HIGH, type, 3, null, groupMembers, null);
    }

    @Test
    void createStartsAsTodoWithCreationTimestampsAndNoCompletion() {
        Task task = Task.create("id-1", "u1", draft(TaskType.ESSAY, null), NOW);

        assertThat(task.getId()).isEqualTo("id-1");
        assertThat(task.getUserId()).isEqualTo("u1");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCreatedAt()).isEqualTo(NOW);
        assertThat(task.getUpdatedAt()).isEqualTo(NOW);
        assertThat(task.getActualTimeHours()).isNull();
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getDescription()).isEmpty();
        assertThat(task.getTags()).isEmpty();
        assertThat(task.getGroupMembers()).isEmpty();
        assertThat(task.getAttachments()).isEmpty();
    }

    @Test
    void transitionUpdatesStatusAndTimestamp() {
        Task task = aTask().build().transitionTo(TaskStatus.IN_PROGRESS, LATER);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getUpdatedAt()).isEqualTo(LATER);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void transitionToDoneSetsCompletedAt() {
        Task task = aTask().status(TaskStatus.IN_REVIEW).build().transitionTo(TaskStatus.DONE, NOW);

        assertThat(task.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void transitionDoesNotMutateTheOriginal() {
        Task original = aTask().build();

        original.transitionTo(TaskStatus.IN_PROGRESS, LATER);

        assertThat(original.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    void finalStatesCannotTransition(TaskStatus finalStatus) {
        Task task = aTask().status(finalStatus).build();

        assertThatThrownBy(() -> task.transitionTo(TaskStatus.IN_PROGRESS, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void todoCannotJumpToDone() {
        assertThatThrownBy(() -> aTask().build().transitionTo(TaskStatus.DONE, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void sameStatusIsAnInvalidTransition() {
        assertThatThrownBy(() -> aTask().build().transitionTo(TaskStatus.TODO, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void transitionTableMatchesTheWorkflow() {
        assertThat(Task.allowedTransitions(TaskStatus.TODO))
                .containsExactlyInAnyOrder(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED);
        assertThat(Task.allowedTransitions(TaskStatus.IN_PROGRESS))
                .containsExactlyInAnyOrder(TaskStatus.TODO, TaskStatus.IN_REVIEW, TaskStatus.DONE, TaskStatus.CANCELLED);
        assertThat(Task.allowedTransitions(TaskStatus.IN_REVIEW))
                .containsExactlyInAnyOrder(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED);
        assertThat(Task.allowedTransitions(TaskStatus.DONE)).isEmpty();
        assertThat(Task.allowedTransitions(TaskStatus.CANCELLED)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    void finalStatesAreNotModifiable(TaskStatus finalStatus) {
        Task task = aTask().status(finalStatus).build();

        assertThat(task.canBeModified()).isFalse();
        assertThatThrownBy(task::ensureModifiable).isInstanceOf(TaskNotModifiableException.class);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"TODO", "IN_PROGRESS", "IN_REVIEW"})
    void openStatesAreModifiable(TaskStatus openStatus) {
        Task task = aTask().status(openStatus).build();

        assertThat(task.canBeModified()).isTrue();
        task.ensureModifiable();
    }

    @Test
    void groupMembersRequireGroupWork() {
        assertThatThrownBy(() -> Task.create("id", "u1", draft(TaskType.ASSIGNMENT, List.of("u2")), NOW))
                .isInstanceOf(InvalidTaskException.class);

        Task task = Task.create("id", "u1", draft(TaskType.GROUP_WORK, List.of("u2")), NOW);
        assertThat(task.getGroupMembers()).containsExactly("u2");
    }

    @Test
    void estimatedHoursCannotBeNegative() {
        TaskDraft negative = new TaskDraft("Essay", null, "c1", "p1", NOW, TaskPriority.LOW,
                TaskType.ESSAY, -1, null, null, null);

        assertThatThrownBy(() -> Task.create("id", "u1", negative, NOW)).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void actualHoursCannotBeNegative() {
        Task task = aTask().build();
        TaskChanges changes = TaskChanges.builder().actualTimeHours(-1).build();

        assertThatThrownBy(() -> task.withChanges(changes, NOW)).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void requiredFieldsMustBePresent() {
        TaskDraft blankTitle = new TaskDraft(" ", null, "c1", "p1", NOW, TaskPriority.LOW,
                TaskType.ESSAY, 1, null, null, null);
        TaskDraft missingCourse = new TaskDraft("Essay", null, null, "p1", NOW, TaskPriority.LOW,
                TaskType.ESSAY, 1, null, null, null);
        TaskDraft missingDueDate = new TaskDraft("Essay", null, "c1", "p1", null, TaskPriority.LOW,
                TaskType.ESSAY, 1, null, null, null);

        assertThatThrownBy(() -> Task.create("id", "u1", blankTitle, NOW)).isInstanceOf(InvalidTaskException.class);
        assertThatThrownBy(() -> Task.create("id", "u1", missingCourse, NOW)).isInstanceOf(InvalidTaskException.class);
        assertThatThrownBy(() -> Task.create("id", "u1", missingDueDate, NOW)).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void nullOrBlankListElementsAreInvalidNotAnError() {
        for (List<String> bad : List.of(Arrays.asList("a", null), List.of(" "))) {
            TaskDraft tags = new TaskDraft("Essay", null, "c1", "p1", NOW, TaskPriority.LOW,
                    TaskType.ESSAY, 1, bad, null, null);
            TaskDraft attachments = new TaskDraft("Essay", null, "c1", "p1", NOW, TaskPriority.LOW,
                    TaskType.ESSAY, 1, null, null, bad);
            assertThatThrownBy(() -> Task.create("id", "u1", tags, NOW)).isInstanceOf(InvalidTaskException.class);
            assertThatThrownBy(() -> Task.create("id", "u1", attachments, NOW)).isInstanceOf(InvalidTaskException.class);
        }
    }

    @Test
    void attachmentsMustBeHttpOrHttpsUrls() {
        for (String bad : List.of("javascript:alert(1)", "data:text/html,x", "ftp://host/file", "http://",
                "file:///etc/passwd", "not a url", "//host/path")) {
            TaskDraft draft = new TaskDraft("Essay", null, "c1", "p1", NOW, TaskPriority.LOW,
                    TaskType.ESSAY, 1, null, null, List.of(bad));
            assertThatThrownBy(() -> Task.create("id", "u1", draft, NOW))
                    .as(bad).isInstanceOf(InvalidTaskException.class);
        }
        TaskDraft good = new TaskDraft("Essay", null, "c1", "p1", NOW, TaskPriority.LOW,
                TaskType.ESSAY, 1, null, null, List.of("https://example.com/a.pdf", "HTTP://example.com/b"));
        assertThat(Task.create("id", "u1", good, NOW).getAttachments()).hasSize(2);
    }

    @Test
    void withChangesAlsoValidatesAttachments() {
        Task task = aTask().build();
        TaskChanges changes = TaskChanges.builder().attachments(List.of("javascript:alert(1)")).build();

        assertThatThrownBy(() -> task.withChanges(changes, NOW)).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void isOverdueOnlyForModifiableTasksPastTheirDueDate() {
        assertThat(aTask().dueDate(NOW.minus(Duration.ofHours(1))).build().isOverdue(NOW)).isTrue();
        assertThat(aTask().build().isOverdue(NOW)).isFalse();
        assertThat(aTask().dueDate(NOW.minus(Duration.ofHours(1))).status(TaskStatus.DONE).build().isOverdue(NOW))
                .isFalse();
        assertThat(aTask().dueDate(NOW).build().isOverdue(NOW)).isFalse();
    }

    @Test
    void isDueSoonWithinTwentyFourHoursInclusive() {
        assertThat(aTask().dueDate(NOW.plus(Duration.ofHours(5))).build().isDueSoon(NOW)).isTrue();
        assertThat(aTask().dueDate(NOW).build().isDueSoon(NOW)).isTrue();
        assertThat(aTask().dueDate(NOW.plus(Duration.ofHours(24))).build().isDueSoon(NOW)).isTrue();
        assertThat(aTask().dueDate(NOW.plus(Duration.ofHours(30))).build().isDueSoon(NOW)).isFalse();
        assertThat(aTask().dueDate(NOW.minus(Duration.ofHours(1))).build().isDueSoon(NOW)).isFalse();
        assertThat(aTask().dueDate(NOW.plus(Duration.ofHours(5))).status(TaskStatus.CANCELLED).build().isDueSoon(NOW))
                .isFalse();
    }

    @Test
    void withChangesReturnsAnUpdatedCopy() {
        Task task = aTask().build();

        Task changed = task.withChanges(TaskChanges.builder().title("New title").build(), LATER);

        assertThat(changed.getTitle()).isEqualTo("New title");
        assertThat(changed.getUpdatedAt()).isEqualTo(LATER);
        assertThat(changed.getCourseId()).isEqualTo("c1");
        assertThat(task.getTitle()).isEqualTo("Homework");
    }

    @Test
    void withChangesCanSetAndClearActualHours() {
        Task task = aTask().build();

        Task withHours = task.withChanges(TaskChanges.builder().actualTimeHours(5).build(), NOW);
        Task cleared = withHours.withChanges(TaskChanges.builder().clearActualTimeHours(true).build(), NOW);
        Task untouched = withHours.withChanges(TaskChanges.builder().title("x").build(), NOW);

        assertThat(withHours.getActualTimeHours()).isEqualTo(5);
        assertThat(cleared.getActualTimeHours()).isNull();
        assertThat(untouched.getActualTimeHours()).isEqualTo(5);
    }

    @Test
    void withChangesRejectsFinalStates() {
        Task done = aTask().status(TaskStatus.DONE).build();

        assertThatThrownBy(() -> done.withChanges(TaskChanges.builder().title("x").build(), NOW))
                .isInstanceOf(TaskNotModifiableException.class);
    }

    @Test
    void withChangesRevalidatesInvariants() {
        Task task = aTask().build();

        assertThatThrownBy(() -> task.withChanges(TaskChanges.builder().groupMembers(List.of("u2")).build(), NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void changingTheTypeAwayFromGroupWorkRequiresNoMembers() {
        Task group = aTask().type(TaskType.GROUP_WORK).groupMembers(List.of("u2")).build();

        assertThatThrownBy(() -> group.withChanges(TaskChanges.builder().type(TaskType.EXAM).build(), NOW))
                .isInstanceOf(InvalidTaskException.class);

        Task cleaned = group.withChanges(
                TaskChanges.builder().type(TaskType.EXAM).groupMembers(List.of()).build(), NOW);
        assertThat(cleaned.getGroupMembers()).isEmpty();
    }
}
