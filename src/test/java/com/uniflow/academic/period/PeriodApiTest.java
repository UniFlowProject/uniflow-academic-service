package com.uniflow.academic.period;

import com.uniflow.academic.period.application.ports.in.ActivatePeriodCommand;
import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeriodApiTest extends IntegrationTestBase {

    @Autowired
    private ActivatePeriodCommand activatePeriodCommand;

    @Test
    void periodIsCreatedAndReadByItsOwner() throws Exception {
        String userA = newUserId();
        String periodId = createPeriod(userA, "2025-1");

        mockMvc.perform(getAs(userA, "/periods/" + periodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(periodId))
                .andExpect(jsonPath("$.userId").value(userA))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void periodsAreScopedPerUser() throws Exception {
        String userA = newUserId();
        String userB = newUserId();
        String periodId = createPeriod(userA, "2025-1");

        mockMvc.perform(getAs(userB, "/periods/" + periodId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
        mockMvc.perform(putAs(userB, "/periods/" + periodId, "{\"name\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(deleteAs(userB, "/periods/" + periodId))
                .andExpect(status().isNotFound());
        mockMvc.perform(patchAs(userB, "/periods/" + periodId + "/activate"))
                .andExpect(status().isNotFound());
        mockMvc.perform(getAs(userB, "/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void listUsesItemsAndPageInfoShape() throws Exception {
        String user = newUserId();
        createPeriod(user, "P1");
        createPeriod(user, "P2");
        createPeriod(user, "P3");

        mockMvc.perform(getAs(user, "/periods?page=1&limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.pageInfo.total").value(3))
                .andExpect(jsonPath("$.pageInfo.page").value(1))
                .andExpect(jsonPath("$.pageInfo.limit").value(2))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(2))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                .andExpect(jsonPath("$.pageInfo.hasPrev").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.pagination").doesNotExist());

        mockMvc.perform(getAs(user, "/periods?page=2&limit=2"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrev").value(true));
    }

    @Test
    void listFiltersByActiveFlagAndType() throws Exception {
        String user = newUserId();
        String first = createPeriod(user, "P1");
        createPeriod(user, "P2");
        mockMvc.perform(patchAs(user, "/periods/" + first + "/activate")).andExpect(status().isOk());

        mockMvc.perform(getAs(user, "/periods?isActive=true&type=first-semester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(first));
    }

    @Test
    void validationErrorUsesSingleErrorBodyWithDetails() throws Exception {
        mockMvc.perform(postAs(newUserId(), "/periods", "{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.details[?(@.field == 'type')]").exists());
    }

    @Test
    void notFoundErrorBodyOmitsNullFields() throws Exception {
        mockMvc.perform(getAs(newUserId(), "/periods/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.details").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void invalidDomainRuleReturnsBadRequest() throws Exception {
        mockMvc.perform(putAs(newUserId(), "/periods/x", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERIOD"));
    }

    @Test
    void malformedJsonReturnsBadRequestBody() throws Exception {
        mockMvc.perform(postAs(newUserId(), "/periods", "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void activatingAnotherPeriodDeactivatesTheCurrentOne() throws Exception {
        String user = newUserId();
        String first = createPeriod(user, "P1");
        String second = createPeriod(user, "P2");

        mockMvc.perform(patchAs(user, "/periods/" + first + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
        mockMvc.perform(patchAs(user, "/periods/" + second + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(getAs(user, "/periods/" + first))
                .andExpect(jsonPath("$.isActive").value(false));
        mockMvc.perform(getAs(user, "/periods/current"))
                .andExpect(jsonPath("$.id").value(second));
    }

    @Test
    void activationServiceDeactivatesBeforeActivatingWithinOneTransaction() throws Exception {
        String user = newUserId();
        String first = createPeriod(user, "P1");
        String second = createPeriod(user, "P2");

        activatePeriodCommand.execute(first, user);
        activatePeriodCommand.execute(second, user);

        Integer active = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic.periods WHERE user_id = ? AND is_active", Integer.class, user);
        assertThat(active).isEqualTo(1);
    }

    @Test
    void activeStateIsIndependentPerUser() throws Exception {
        String userA = newUserId();
        String userB = newUserId();
        String periodA = createPeriod(userA, "P1");
        String periodB = createPeriod(userB, "P1");

        mockMvc.perform(patchAs(userA, "/periods/" + periodA + "/activate")).andExpect(status().isOk());
        mockMvc.perform(patchAs(userB, "/periods/" + periodB + "/activate")).andExpect(status().isOk());

        mockMvc.perform(getAs(userA, "/periods/current")).andExpect(jsonPath("$.id").value(periodA));
        mockMvc.perform(getAs(userB, "/periods/current")).andExpect(jsonPath("$.id").value(periodB));
    }

    @Test
    void databaseRejectsTwoActivePeriodsForTheSameUser() throws Exception {
        String user = newUserId();
        String first = createPeriod(user, "P1");
        String second = createPeriod(user, "P2");
        jdbcTemplate.update("UPDATE academic.periods SET is_active = TRUE WHERE id = ?", first);

        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE academic.periods SET is_active = TRUE WHERE id = ?", second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void periodWithCoursesCannotBeDeleted() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated());

        mockMvc.perform(deleteAs(user, "/periods/" + periodId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_HAS_COURSES"));
    }

    @Test
    void emptyPeriodCanBeDeleted() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(deleteAs(user, "/periods/" + periodId)).andExpect(status().isNoContent());
        mockMvc.perform(getAs(user, "/periods/" + periodId)).andExpect(status().isNotFound());
    }

    private String periodBody(String start, String end) {
        return """
                {"name":"P","type":"first-semester","year":2025,"startDate":"%s","endDate":"%s"}
                """.formatted(start, end);
    }

    @Test
    void periodWithEqualStartAndEndDateIsValid() throws Exception {
        String user = newUserId();

        mockMvc.perform(postAs(user, "/periods", periodBody("2025-03-01", "2025-03-01")))
                .andExpect(status().isCreated());

        String periodId = createPeriod(user, "P1");
        mockMvc.perform(putAs(user, "/periods/" + periodId,
                        "{\"startDate\":\"2025-06-20\",\"endDate\":\"2025-06-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2025-06-20"))
                .andExpect(jsonPath("$.endDate").value("2025-06-20"));
    }

    @Test
    void periodWithStartAfterEndIsRejectedOnCreate() throws Exception {
        mockMvc.perform(postAs(newUserId(), "/periods", periodBody("2025-06-21", "2025-06-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERIOD"))
                .andExpect(jsonPath("$.error.message").value("Start date must not be after end date"));
    }

    @Test
    void periodWithStartAfterEndIsRejectedOnUpdate() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(putAs(user, "/periods/" + periodId,
                        "{\"startDate\":\"2025-08-01\",\"endDate\":\"2025-07-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERIOD"));
    }

    @Test
    void singleFieldUpdateThatInvertsTheDatesIsRejected() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(putAs(user, "/periods/" + periodId, "{\"startDate\":\"2025-07-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERIOD"));
        mockMvc.perform(getAs(user, "/periods/" + periodId))
                .andExpect(jsonPath("$.startDate").value("2025-02-03"));
    }

    @Test
    void invalidPaginationParametersAreRejected() throws Exception {
        String user = newUserId();

        for (String query : new String[]{"page=0", "page=-3", "limit=0", "limit=101", "limit=-1"}) {
            mockMvc.perform(getAs(user, "/periods?" + query))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(getAs(user, "/periods?page=1&limit=100")).andExpect(status().isOk());
    }

    private void createPeriodOfYear(String user, int year) throws Exception {
        mockMvc.perform(postAs(user, "/periods", """
                        {"name":"Y%d","type":"summer","year":%d,"startDate":"%d-01-10","endDate":"%d-03-10"}
                        """.formatted(year, year, year, year)))
                .andExpect(status().isCreated());
    }

    @Test
    void putUpdatesAllProvidedFields() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(putAs(user, "/periods/" + periodId, """
                        {"name":"Renamed","type":"summer","year":2026,
                         "startDate":"2026-01-10","endDate":"2026-03-10"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.type").value("summer"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.startDate").value("2026-01-10"))
                .andExpect(jsonPath("$.endDate").value("2026-03-10"));
        mockMvc.perform(getAs(user, "/periods/" + periodId))
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void blankNameOnUpdateIsRejected() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(putAs(user, "/periods/" + periodId, "{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'name')]").exists());
        mockMvc.perform(getAs(user, "/periods/" + periodId))
                .andExpect(jsonPath("$.name").value("P1"));
    }

    @Test
    void invalidTypeOnUpdateIsRejected() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(putAs(user, "/periods/" + periodId, "{\"type\":\"winter\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'type')]").exists());
    }

    @Test
    void currentPeriodWithoutActiveOneIsNotFound() throws Exception {
        String user = newUserId();
        createPeriod(user, "P1");

        mockMvc.perform(getAs(user, "/periods/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
    }

    @Test
    void listFiltersByYearAndActiveFlagAndCombination() throws Exception {
        String user = newUserId();
        String active = createPeriod(user, "Active2025");
        createPeriodOfYear(user, 2024);
        createPeriodOfYear(user, 2026);
        mockMvc.perform(patchAs(user, "/periods/" + active + "/activate")).andExpect(status().isOk());

        mockMvc.perform(getAs(user, "/periods?year=2024"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].year").value(2024));
        mockMvc.perform(getAs(user, "/periods?isActive=false"))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mockMvc.perform(getAs(user, "/periods?isActive=true&year=2025"))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(getAs(user, "/periods?isActive=true&year=2024"))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(getAs(user, "/periods?isActive=false&type=summer&year=2026"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void pageBeyondTheLastIsEmptyWithoutNext() throws Exception {
        String user = newUserId();
        createPeriod(user, "P1");
        createPeriod(user, "P2");

        mockMvc.perform(getAs(user, "/periods?page=5&limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.pageInfo.total").value(2))
                .andExpect(jsonPath("$.pageInfo.page").value(5))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrev").value(true));
    }

    @Test
    void emptyListHasZeroTotalPages() throws Exception {
        mockMvc.perform(getAs(newUserId(), "/periods"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.pageInfo.total").value(0))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(0))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrev").value(false));
    }
}
