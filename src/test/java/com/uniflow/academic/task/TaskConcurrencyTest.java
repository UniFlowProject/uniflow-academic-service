package com.uniflow.academic.task;

import com.uniflow.academic.support.IntegrationTestBase;
import com.uniflow.academic.task.application.ports.in.ChangeTaskStatusCommand;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskChanges;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository repository;

    @Autowired
    private ChangeTaskStatusCommand changeStatus;

    @Autowired
    private DataSource dataSource;

    private String user;
    private Task task;

    @BeforeEach
    void setUp() throws Exception {
        user = newUserId();
        String periodId = createPeriod(user, "P1");
        String courseId = createCourse(user, periodId, "MAT-101");
        task = repository.save(aTask().id(java.util.UUID.randomUUID().toString()).userId(user)
                .courseId(courseId).periodId(periodId).build());
    }

    private static TaskChanges title(String title) {
        return TaskChanges.builder().title(title).build();
    }

    private Task retitled(Task loaded, String title) {
        return loaded.withChanges(title(title), Instant.now());
    }

    private String titleInDb() {
        return jdbcTemplate.queryForObject("SELECT title FROM academic.tasks WHERE id = ?", String.class, task.getId());
    }

    @Test
    void firstSaveStartsAtVersionZeroAndEachUpdateIncrementsIt() {
        assertThat(task.getVersion()).isZero();

        Task updated = repository.save(retitled(task, "v1"));

        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(repository.findById(task.getId(), user).orElseThrow().getVersion()).isEqualTo(1);
    }

    @Test
    void secondOfTwoUpdatesFromTheSameVersionIsRejected() {
        Task copyA = repository.findById(task.getId(), user).orElseThrow();
        Task copyB = repository.findById(task.getId(), user).orElseThrow();

        repository.save(retitled(copyA, "A"));

        assertThatThrownBy(() -> repository.save(retitled(copyB, "B")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(titleInDb()).isEqualTo("A");
    }

    @Test
    void statusChangeInvalidatesAConcurrentlyLoadedCopy() {
        Task stale = repository.findById(task.getId(), user).orElseThrow();

        changeStatus.execute(task.getId(), TaskStatus.IN_PROGRESS, user);

        assertThatThrownBy(() -> repository.save(retitled(stale, "late edit")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(repository.findById(task.getId(), user).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void savingADeletedTaskIsNotFoundAndDoesNotResurrectTheRow() {
        Task stale = repository.findById(task.getId(), user).orElseThrow();
        repository.delete(task.getId(), user);

        assertThatThrownBy(() -> repository.save(retitled(stale, "ghost")))
                .isInstanceOf(TaskNotFoundException.class);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic.tasks WHERE id = ?", Integer.class, task.getId());
        assertThat(rows).isZero();
    }

    @Test
    void patchOfADeletedTaskIsNotFound() throws Exception {
        mockMvc.perform(deleteAs(user, "/tasks/" + task.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(patchAs(user, "/tasks/" + task.getId()).contentType("application/json")
                        .content("{\"title\":\"ghost\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic.tasks WHERE id = ?", Integer.class, task.getId());
        assertThat(rows).isZero();
    }

    /**
     * A competing transaction bumps the row and stays uncommitted; the PATCH loads the old version and
     * blocks on the row lock. Once the competitor commits, the update affects no row and the request
     * must be a 409 TASK_CONFLICT, not a lost update or a 500.
     */
    @Test
    void concurrentPatchLosingTheRaceGetsATaskConflict() throws Exception {
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (var statement = competitor.prepareStatement(
                    "UPDATE academic.tasks SET title = 'winner', version = version + 1 WHERE id = ?")) {
                statement.setString(1, task.getId());
                statement.executeUpdate();
            }

            CompletableFuture<MvcResult> loser = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(patchAs(user, "/tasks/" + task.getId()).contentType("application/json")
                            .content("{\"title\":\"loser\"}")).andReturn();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            awaitBlockedOnLock();
            competitor.commit();

            MvcResult result = loser.get(10, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(json(result.getResponse().getContentAsString()).at("/error/code").asText())
                    .isEqualTo("TASK_CONFLICT");
        }

        assertThat(titleInDb()).isEqualTo("winner");
    }

    private void awaitBlockedOnLock() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Integer waiting = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
                    Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The update never blocked on the row lock");
    }
}
