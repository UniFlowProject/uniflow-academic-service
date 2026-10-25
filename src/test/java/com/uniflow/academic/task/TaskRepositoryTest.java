package com.uniflow.academic.task;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.support.IntegrationTestBase;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.application.ports.out.dto.SortOrder;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.application.ports.out.dto.TaskSortField;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStats;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRepositoryTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository repository;

    private String user;
    private String periodId;
    private String courseId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM academic.tasks");
        user = newUserId();
        periodId = createPeriod(user, "P1");
        courseId = createCourse(user, periodId, "MAT-101");
    }

    private Task save(String id, UnaryOperator<Task.TaskBuilder> customize) {
        return repository.save(customize.apply(
                aTask().id(id).userId(user).courseId(courseId).periodId(periodId)).build());
    }

    private Task save(String id) {
        return save(id, b -> b);
    }

    private List<String> ids(PagedResult<Task> page) {
        return page.items().stream().map(Task::getId).toList();
    }

    private PagedResult<Task> search(TaskFilter filter) {
        return search(filter, PaginationParams.of(1, 100, 20));
    }

    private PagedResult<Task> search(TaskFilter filter, PaginationParams pagination) {
        return repository.search(user, filter, pagination, NOW);
    }

    private TaskFilter.TaskFilterBuilder filter() {
        return TaskFilter.builder();
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void savedTaskRoundTripsEveryField() {
        Task task = repository.save(aTask().id(newId()).userId(user).courseId(courseId).periodId(periodId)
                .description("desc").actualTimeHours(3).status(TaskStatus.DONE).completedAt(NOW)
                .type(TaskType.GROUP_WORK).tags(List.of("a", "b")).groupMembers(List.of("u2"))
                .attachments(List.of("x.pdf")).build());

        assertThat(repository.findById(task.getId(), user)).contains(task);
    }

    @Test
    void emptyListsAndNullOptionalsRoundTrip() {
        Task task = save(newId());

        Task found = repository.findById(task.getId(), user).orElseThrow();

        assertThat(found.getTags()).isEmpty();
        assertThat(found.getGroupMembers()).isEmpty();
        assertThat(found.getAttachments()).isEmpty();
        assertThat(found.getActualTimeHours()).isNull();
        assertThat(found.getCompletedAt()).isNull();
    }

    @Test
    void saveUpsertsAnExistingTask() {
        Task saved = save(newId());
        repository.save(saved.toBuilder().title("renamed").build());
        String id = saved.getId();

        assertThat(repository.findById(id, user)).get().extracting(Task::getTitle).isEqualTo("renamed");
        assertThat(search(filter().build()).items()).hasSize(1);
    }

    @Test
    void unknownTaskIsEmpty() {
        assertThat(repository.findById("missing", user)).isEmpty();
    }

    @Test
    void foreignUserCannotReadOrDelete() {
        Task task = save(newId());

        assertThat(repository.findById(task.getId(), newUserId())).isEmpty();
        assertThat(repository.delete(task.getId(), newUserId())).isFalse();
        assertThat(repository.findById(task.getId(), user)).isPresent();
    }

    @Test
    void deleteRemovesTheTaskAndReportsMissingOnes() {
        Task task = save(newId());

        assertThat(repository.delete(task.getId(), user)).isTrue();
        assertThat(repository.delete(task.getId(), user)).isFalse();
        assertThat(repository.findById(task.getId(), user)).isEmpty();
    }

    @Test
    void statsCountEveryStatusPriorityTypeAndCourse() throws Exception {
        String otherCourse = createCourse(user, periodId, "FIS-101");
        save("done", b -> b.status(TaskStatus.DONE));
        save("cancelled", b -> b.status(TaskStatus.CANCELLED).dueDate(NOW.minus(Duration.ofDays(1))));
        save("late", b -> b.dueDate(NOW.minus(Duration.ofDays(1))).priority(TaskPriority.URGENT));
        save("review", b -> b.status(TaskStatus.IN_REVIEW).type(TaskType.EXAM));
        repository.save(aTask().id("other").userId(user).courseId(otherCourse).periodId(periodId).build());

        TaskStats stats = repository.stats(user, NOW);

        assertThat(stats.total()).isEqualTo(5);
        assertThat(stats.completed()).isEqualTo(1);
        assertThat(stats.pending()).isEqualTo(3);
        assertThat(stats.overdue()).isEqualTo(1);
        assertThat(stats.byStatus()).isEqualTo(Map.of("todo", 2L, "done", 1L, "cancelled", 1L, "in-review", 1L));
        assertThat(stats.byPriority()).isEqualTo(Map.of("medium", 4L, "urgent", 1L));
        assertThat(stats.byType()).isEqualTo(Map.of("assignment", 4L, "exam", 1L));
        assertThat(stats.byCourse()).isEqualTo(Map.of(courseId, 4L, otherCourse, 1L));
    }

    @Test
    void statsOfAUserWithoutTasksAreEmpty() {
        TaskStats stats = repository.stats(newUserId(), NOW);

        assertThat(stats.total()).isZero();
        assertThat(stats.completed()).isZero();
        assertThat(stats.pending()).isZero();
        assertThat(stats.overdue()).isZero();
        assertThat(stats.byStatus()).isEmpty();
        assertThat(stats.byPriority()).isEmpty();
        assertThat(stats.byType()).isEmpty();
        assertThat(stats.byCourse()).isEmpty();
    }

    @Test
    void statsCoverHundredsOfRowsAndStayScopedToTheUser() throws Exception {
        String other = newUserId();
        String otherPeriod = createPeriod(other, "P1");
        String otherCourse = createCourse(other, otherPeriod, "MAT-101");
        for (int n = 0; n < 250; n++) {
            TaskStatus status = n % 5 == 0 ? TaskStatus.DONE : TaskStatus.TODO;
            Instant due = n % 2 == 0 ? NOW.minus(Duration.ofDays(1)) : NOW.plus(Duration.ofDays(1));
            save(newId(), b -> b.status(status).dueDate(due));
        }
        for (int n = 0; n < 100; n++) {
            repository.save(aTask().id(newId()).userId(other).courseId(otherCourse).periodId(otherPeriod).build());
        }

        TaskStats stats = repository.stats(user, NOW);

        assertThat(stats.total()).isEqualTo(250);
        assertThat(stats.completed()).isEqualTo(50);
        assertThat(stats.pending()).isEqualTo(200);
        assertThat(stats.overdue()).isEqualTo(100);
        assertThat(stats.byCourse()).isEqualTo(Map.of(courseId, 250L));
        assertThat(repository.stats(other, NOW).total()).isEqualTo(100);
    }

    @Test
    void listIsScopedToTheUser() throws Exception {
        save("a");
        String other = newUserId();
        String otherPeriod = createPeriod(other, "P1");
        String otherCourse = createCourse(other, otherPeriod, "MAT-101");
        repository.save(aTask().id("b").userId(other).courseId(otherCourse).periodId(otherPeriod).build());

        PagedResult<Task> page = search(filter().build());

        assertThat(ids(page)).containsExactly("a");
        assertThat(page.pageInfo().total()).isEqualTo(1);
    }

    @Test
    void listFiltersByStatusAndPriority() {
        save("a", b -> b.status(TaskStatus.DONE));
        save("b", b -> b.priority(TaskPriority.HIGH));
        save("c", b -> b);

        assertThat(ids(search(filter().statuses(List.of(TaskStatus.DONE)).build()))).containsExactly("a");
        assertThat(ids(search(filter().priorities(List.of(TaskPriority.HIGH)).build()))).containsExactly("b");
        assertThat(ids(search(filter().statuses(List.of(TaskStatus.TODO, TaskStatus.DONE)).build())))
                .containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void emptyStatusAndPriorityListsMeanNoFilter() {
        save("a", b -> b.status(TaskStatus.DONE));
        save("b", b -> b.priority(TaskPriority.HIGH));

        assertThat(ids(search(filter().statuses(List.of()).priorities(List.of()).build())))
                .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void listFiltersByCourseAndPeriod() throws Exception {
        String otherPeriod = createPeriod(user, "P2");
        String otherCourse = createCourse(user, otherPeriod, "FIS-101");
        save("a");
        repository.save(aTask().id("b").userId(user).courseId(otherCourse).periodId(otherPeriod).build());

        assertThat(ids(search(filter().courseId(otherCourse).build()))).containsExactly("b");
        assertThat(ids(search(filter().periodId(otherPeriod).build()))).containsExactly("b");
        assertThat(ids(search(filter().periodId(periodId).build()))).containsExactly("a");
    }

    @Test
    void listFiltersByInclusiveDueDateRange() {
        save("early", b -> b.dueDate(NOW.plus(Duration.ofDays(1))));
        save("from", b -> b.dueDate(NOW.plus(Duration.ofDays(2))));
        save("mid", b -> b.dueDate(NOW.plus(Duration.ofDays(4))));
        save("to", b -> b.dueDate(NOW.plus(Duration.ofDays(6))));
        save("late", b -> b.dueDate(NOW.plus(Duration.ofDays(9))));

        TaskFilter range = filter()
                .dueDateFrom(NOW.plus(Duration.ofDays(2)))
                .dueDateTo(NOW.plus(Duration.ofDays(6)))
                .build();

        assertThat(ids(search(range))).containsExactly("from", "mid", "to");
    }

    @Test
    void overdueFilterUsesTheGivenNowAndIgnoresFinishedTasks() {
        save("late", b -> b.dueDate(NOW.minus(Duration.ofDays(1))));
        save("ok", b -> b.dueDate(NOW.plus(Duration.ofDays(1))));
        save("done", b -> b.dueDate(NOW.minus(Duration.ofDays(1))).status(TaskStatus.DONE));
        save("now", b -> b.dueDate(NOW));

        assertThat(ids(search(filter().overdue(true).build()))).containsExactly("late");
        assertThat(ids(search(filter().overdue(false).build()))).containsExactlyInAnyOrder("ok", "done", "now");
    }

    @Test
    void dueSoonFilterCoversTheNextTwentyFourHours() {
        save("soon", b -> b.dueDate(NOW.plus(Duration.ofHours(3))));
        save("edge", b -> b.dueDate(NOW.plus(Duration.ofHours(24))));
        save("far", b -> b.dueDate(NOW.plus(Duration.ofDays(3))));
        save("past", b -> b.dueDate(NOW.minus(Duration.ofHours(1))));
        save("cancelled", b -> b.dueDate(NOW.plus(Duration.ofHours(3))).status(TaskStatus.CANCELLED));

        assertThat(ids(search(filter().dueSoon(true).build()))).containsExactlyInAnyOrder("soon", "edge");
        assertThat(ids(search(filter().dueSoon(false).build())))
                .containsExactlyInAnyOrder("far", "past", "cancelled");
    }

    @Test
    void searchMatchesTitleOrDescriptionIgnoringCase() {
        save("a", b -> b.title("Calculus HOMEWORK"));
        save("b", b -> b.title("Other").description("some homework here"));
        save("c", b -> b.title("Unrelated"));

        assertThat(ids(search(filter().search("homework").build()))).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void searchMatchesTermsSpanningTitleAndDescription() {
        save("a", b -> b.title("Calculus").description("homework"));
        save("b", b -> b.title("Other").description("nothing"));

        assertThat(ids(search(filter().search("calculus homework").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("us ho").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("calculushomework").build()))).isEmpty();
    }

    @Test
    void searchKeepsLeadingAndTrailingSpacesOfTheTerm() {
        save("a", b -> b.title("Read").description("chapter"));
        save("b", b -> b.title("Reader").description(""));
        save("c", b -> b.title("Ready").description("").title("xchapter"));

        assertThat(ids(search(filter().search(" chapter").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("read ").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("reader ").build()))).containsExactly("b");
    }

    @Test
    void searchIsCaseInsensitiveForNonAsciiText() {
        save("a", b -> b.title("Ejercicio de ÁRBOLES").description("Año académico ÑANDÚ"));
        save("b", b -> b.title("Otro"));

        assertThat(ids(search(filter().search("árboles").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("ÁRBOLES").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("AÑO ACADÉMICO").build()))).containsExactly("a");
        assertThat(ids(search(filter().search("ñandú").build()))).containsExactly("a");
    }

    @Test
    void searchTreatsLikeWildcardsAsLiterals() {
        save("percent", b -> b.title("100% done"));
        save("underscore", b -> b.title("snake_case"));
        save("backslash", b -> b.title("path\\to"));
        save("plain", b -> b.title("1000 done snakeXcase pathXto"));

        assertThat(ids(search(filter().search("100%").build()))).containsExactly("percent");
        assertThat(ids(search(filter().search("e_c").build()))).containsExactly("underscore");
        assertThat(ids(search(filter().search("h\\t").build()))).containsExactly("backslash");
    }

    @Test
    void blankSearchIsIgnored() {
        save("a");

        assertThat(ids(search(filter().search("   ").build()))).containsExactly("a");
    }

    @Test
    void sortsByDueDateAscendingByDefault() {
        save("b", b -> b.dueDate(NOW.plus(Duration.ofDays(2))));
        save("a", b -> b.dueDate(NOW.plus(Duration.ofDays(1))));

        assertThat(ids(search(filter().build()))).containsExactly("a", "b");
        assertThat(ids(search(filter().sortOrder(SortOrder.DESC).build()))).containsExactly("b", "a");
    }

    @Test
    void sortsByPriorityRankNotAlphabetically() {
        save("low", b -> b.priority(TaskPriority.LOW));
        save("urgent", b -> b.priority(TaskPriority.URGENT));
        save("high", b -> b.priority(TaskPriority.HIGH));
        save("medium", b -> b.priority(TaskPriority.MEDIUM));

        assertThat(ids(search(filter().sortBy(TaskSortField.PRIORITY).sortOrder(SortOrder.DESC).build())))
                .containsExactly("urgent", "high", "medium", "low");
        assertThat(ids(search(filter().sortBy(TaskSortField.PRIORITY).sortOrder(SortOrder.ASC).build())))
                .containsExactly("low", "medium", "high", "urgent");
    }

    @Test
    void sortsByStatusWorkflowOrderNotAlphabetically() {
        save("done", b -> b.status(TaskStatus.DONE));
        save("todo", b -> b.status(TaskStatus.TODO));
        save("review", b -> b.status(TaskStatus.IN_REVIEW));
        save("progress", b -> b.status(TaskStatus.IN_PROGRESS));
        save("cancelled", b -> b.status(TaskStatus.CANCELLED));

        assertThat(ids(search(filter().sortBy(TaskSortField.STATUS).build())))
                .containsExactly("todo", "progress", "review", "done", "cancelled");
    }

    @Test
    void sortsByCreatedAt() {
        save("new", b -> b.createdAt(NOW.plus(Duration.ofHours(1))));
        save("old", b -> b.createdAt(NOW));

        assertThat(ids(search(filter().sortBy(TaskSortField.CREATED_AT).sortOrder(SortOrder.DESC).build())))
                .containsExactly("new", "old");
    }

    @Test
    void equalSortKeysFallBackToCreationTimeThenId() {
        save("c", b -> b.createdAt(NOW.plus(Duration.ofHours(2))));
        save("b", b -> b.createdAt(NOW.plus(Duration.ofHours(1))));
        save("a2", b -> b.createdAt(NOW));
        save("a1", b -> b.createdAt(NOW));

        assertThat(ids(search(filter().sortBy(TaskSortField.PRIORITY).build())))
                .containsExactly("a1", "a2", "b", "c");
    }

    @Test
    void paginatesAfterFilteringAndSorting() {
        for (int n = 0; n < 5; n++) {
            int days = n + 1;
            save("t" + n, b -> b.dueDate(NOW.plus(Duration.ofDays(days))));
        }

        PagedResult<Task> page = search(filter().build(), PaginationParams.of(2, 2));

        assertThat(ids(page)).containsExactly("t2", "t3");
        assertThat(page.pageInfo().total()).isEqualTo(5);
        assertThat(page.pageInfo().totalPages()).isEqualTo(3);
        assertThat(page.pageInfo().hasNext()).isTrue();
        assertThat(page.pageInfo().hasPrev()).isTrue();
    }

    @Test
    void pageBeyondTheLastIsEmptyButKeepsTheTotal() {
        save("a");

        PagedResult<Task> page = search(filter().build(), PaginationParams.of(3, 10));

        assertThat(page.items()).isEmpty();
        assertThat(page.pageInfo().total()).isEqualTo(1);
        assertThat(page.pageInfo().hasNext()).isFalse();
    }

    @Test
    void emptyResultHasNoPages() {
        PagedResult<Task> page = search(filter().build());

        assertThat(page.items()).isEmpty();
        assertThat(page.pageInfo().total()).isZero();
        assertThat(page.pageInfo().totalPages()).isZero();
    }

    @Test
    void databaseRejectsADeletedCourseWithTasks() {
        save("a");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM academic.courses WHERE id = ?", courseId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsATaskWhoseCourseBelongsToAnotherPeriod() throws Exception {
        String otherPeriod = createPeriod(user, "P2");

        assertThatThrownBy(() -> repository.save(
                aTask().id(newId()).userId(user).courseId(courseId).periodId(otherPeriod).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsGroupMembersOnNonGroupWork() {
        assertThatThrownBy(() -> repository.save(aTask().id(newId()).userId(user).courseId(courseId)
                .periodId(periodId).type(TaskType.EXAM).groupMembers(List.of("u2")).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNegativeHours() {
        assertThatThrownBy(() -> repository.save(aTask().id(newId()).userId(user).courseId(courseId)
                .periodId(periodId).estimatedTimeHours(-1).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> repository.save(aTask().id(newId()).userId(user).courseId(courseId)
                .periodId(periodId).actualTimeHours(-1).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void instantsKeepTheirPrecisionToTheMicrosecond() {
        Instant due = Instant.parse("2026-11-01T10:00:00.123456Z");
        Task task = save(newId(), b -> b.dueDate(due));

        assertThat(repository.findById(task.getId(), user)).get().extracting(Task::getDueDate).isEqualTo(due);
    }
}
