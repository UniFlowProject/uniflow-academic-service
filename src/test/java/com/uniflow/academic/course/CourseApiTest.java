package com.uniflow.academic.course;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseApiTest extends IntegrationTestBase {

    @Test
    void courseIsCreatedInAnOwnedPeriod() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodId").value(periodId))
                .andExpect(jsonPath("$.code").value("MAT-101"));

        mockMvc.perform(getAs(user, "/periods/" + periodId + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses", hasSize(1)));
    }

    @Test
    void courseCannotBeCreatedInAnotherUsersPeriod() throws Exception {
        String owner = newUserId();
        String intruder = newUserId();
        String periodId = createPeriod(owner, "P1");

        mockMvc.perform(postAs(intruder, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
        mockMvc.perform(getAs(owner, "/courses"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void courseCannotBeCreatedInAMissingPeriod() throws Exception {
        mockMvc.perform(postAs(newUserId(), "/courses", courseJson("missing-period", "MAT-101")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
    }

    @Test
    void courseListFilteredByAnotherUsersPeriodIsNotFound() throws Exception {
        String periodId = createPeriod(newUserId(), "P1");

        mockMvc.perform(getAs(newUserId(), "/courses?periodId=" + periodId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
    }

    @Test
    void periodCourseListOfAnotherUsersPeriodIsNotFound() throws Exception {
        String periodId = createPeriod(newUserId(), "P1");

        mockMvc.perform(getAs(newUserId(), "/periods/" + periodId + "/courses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
    }

    @Test
    void coursesAreScopedPerUser() throws Exception {
        String owner = newUserId();
        String other = newUserId();
        String periodId = createPeriod(owner, "P1");
        String body = mockMvc.perform(postAs(owner, "/courses", courseJson(periodId, "MAT-101")))
                .andReturn().getResponse().getContentAsString();
        String courseId = json(body).get("id").asText();

        mockMvc.perform(getAs(other, "/courses/" + courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(getAs(other, "/courses"))
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(deleteAs(other, "/courses/" + courseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void courseCodeIsUniquePerUserAndPeriodIgnoringCase() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        String otherPeriodId = createPeriod(user, "P2");

        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated());
        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "mat-101")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COURSE_CODE_ALREADY_EXISTS"));
        mockMvc.perform(postAs(user, "/courses", courseJson(otherPeriodId, "MAT-101")))
                .andExpect(status().isCreated());
    }

    @Test
    void sameCodeIsAllowedForDifferentUsers() throws Exception {
        String userA = newUserId();
        String userB = newUserId();

        mockMvc.perform(postAs(userA, "/courses", courseJson(createPeriod(userA, "P1"), "MAT-101")))
                .andExpect(status().isCreated());
        mockMvc.perform(postAs(userB, "/courses", courseJson(createPeriod(userB, "P1"), "MAT-101")))
                .andExpect(status().isCreated());
    }

    @Test
    void databaseEnforcesCaseInsensitiveCodeUniqueness() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        String insert = "INSERT INTO academic.courses (id, name, code, period_id, user_id) VALUES (?, 'N', ?, ?, ?)";
        jdbcTemplate.update(insert, newUserId(), "ABC-100", periodId, user);

        assertThatThrownBy(() -> jdbcTemplate.update(insert, newUserId(), "abc-100", periodId, user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void validateCodeReportsAvailability() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated());

        mockMvc.perform(postAs(user, "/courses/validate-code",
                        "{\"code\":\"MAT-101\",\"periodId\":\"%s\"}".formatted(periodId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(postAs(user, "/courses/validate-code",
                        "{\"code\":\"FIS-202\",\"periodId\":\"%s\"}".formatted(periodId)))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void statisticsCountOnlyTheCallersCourses() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated());

        mockMvc.perform(getAs(user, "/stats/courses")).andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(getAs(newUserId(), "/stats/courses")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void periodIdIsTrimmedOnCreateAndListFilter() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(postAs(user, "/courses", courseJson(" " + periodId + " ", "MAT-101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodId").value(periodId));
        mockMvc.perform(getAs(user, "/courses").param("periodId", " " + periodId + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
