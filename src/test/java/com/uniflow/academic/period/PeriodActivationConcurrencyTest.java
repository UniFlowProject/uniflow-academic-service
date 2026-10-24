package com.uniflow.academic.period;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeriodActivationConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    /**
     * A concurrent transaction activates period A and stays uncommitted; the API activation of B
     * blocks on the partial unique index. Once A commits, B's insert-time check fails and the
     * request must be translated to a 409 instead of a 500.
     */
    @Test
    void loserOfAConcurrentActivationGetsAConflictAndExactlyOnePeriodStaysActive() throws Exception {
        String user = newUserId();
        String periodA = createPeriod(user, "A");
        String periodB = createPeriod(user, "B");

        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (var statement = competitor.prepareStatement(
                    "UPDATE academic.periods SET is_active = TRUE WHERE id = ?")) {
                statement.setString(1, periodA);
                statement.executeUpdate();
            }

            CompletableFuture<MvcResult> loser = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(patchAs(user, "/periods/" + periodB + "/activate")).andReturn();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            awaitBlockedOnLock();
            competitor.commit();

            MvcResult result = loser.get(10, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(json(result.getResponse().getContentAsString()).at("/error/code").asText())
                    .isEqualTo("PERIOD_ACTIVATION_CONFLICT");
        }

        Integer active = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic.periods WHERE user_id = ? AND is_active", Integer.class, user);
        assertThat(active).isEqualTo(1);
        mockMvc.perform(getAs(user, "/periods/current")).andExpect(jsonPath("$.id").value(periodA));
        mockMvc.perform(patchAs(user, "/periods/" + periodB + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
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
        throw new AssertionError("The activation request never blocked on the unique index");
    }
}
