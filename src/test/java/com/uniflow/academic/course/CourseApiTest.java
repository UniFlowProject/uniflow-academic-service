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
                .andExpect(jsonPath("$.items", hasSize(1)));
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
                .andExpect(jsonPath("$.items", hasSize(0)));
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
                .andExpect(jsonPath("$.items", hasSize(0)));
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
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    private String createCourseWith(String user, String periodId, String code, String professor, int credits, String name)
            throws Exception {
        String body = mockMvc.perform(postAs(user, "/courses", """
                        {"name":"%s","code":"%s","professor":"%s","credits":%d,"periodId":"%s"}
                        """.formatted(name, code, professor, credits, periodId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }

    @Test
    void putReplacesTheCourse() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        mockMvc.perform(putAs(user, "/courses/" + courseId,
                        "{\"name\":\"Algebra\",\"code\":\"ALG-200\",\"credits\":3,\"professor\":\"Ada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Algebra"))
                .andExpect(jsonPath("$.code").value("ALG-200"))
                .andExpect(jsonPath("$.credits").value(3))
                .andExpect(jsonPath("$.professor").value("Ada"));
        mockMvc.perform(getAs(user, "/courses/" + courseId))
                .andExpect(jsonPath("$.code").value("ALG-200"));
    }

    @Test
    void putOfUnknownOrForeignCourseIsNotFound() throws Exception {
        String owner = newUserId();
        String courseId = createCourse(owner, createPeriod(owner, "P1"), "MAT-101");
        String body = "{\"name\":\"X\",\"code\":\"XXX-100\",\"credits\":3}";

        mockMvc.perform(putAs(newUserId(), "/courses/" + courseId, body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(putAs(owner, "/courses/missing", body)).andExpect(status().isNotFound());
    }

    @Test
    void patchUpdatesOnlyTheProvidedFields() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        mockMvc.perform(patchAs(user, "/courses/" + courseId).contentType("application/json")
                        .content("{\"professor\":\"Grace\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professor").value("Grace"))
                .andExpect(jsonPath("$.name").value("Calculus"))
                .andExpect(jsonPath("$.code").value("MAT-101"));
    }

    @Test
    void patchKeepingTheCoursesOwnCodeIsAllowed() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        mockMvc.perform(patchAs(user, "/courses/" + courseId).contentType("application/json")
                        .content("{\"code\":\"mat-101\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patchOfUnknownOrForeignCourseIsNotFound() throws Exception {
        String owner = newUserId();
        String courseId = createCourse(owner, createPeriod(owner, "P1"), "MAT-101");

        mockMvc.perform(patchAs(newUserId(), "/courses/" + courseId).contentType("application/json")
                        .content("{\"professor\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(patchAs(owner, "/courses/missing").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesTheCourse() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        mockMvc.perform(deleteAs(user, "/courses/" + courseId)).andExpect(status().isNoContent());
        mockMvc.perform(getAs(user, "/courses/" + courseId)).andExpect(status().isNotFound());
        mockMvc.perform(deleteAs(user, "/courses/" + courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void putRejectsBlankFieldsAndNegativeCredits() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        for (String body : new String[]{
                "{\"name\":\" \",\"code\":\"ABC-100\",\"credits\":3}",
                "{\"name\":\"N\",\"code\":\"\",\"credits\":3}",
                "{\"name\":\"N\",\"code\":\"ABC-100\",\"credits\":-1}"}) {
            mockMvc.perform(putAs(user, "/courses/" + courseId, body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void patchRejectsBlankFieldsAndNegativeCredits() throws Exception {
        String user = newUserId();
        String courseId = createCourse(user, createPeriod(user, "P1"), "MAT-101");

        for (String body : new String[]{"{\"name\":\"  \"}", "{\"code\":\"\"}", "{\"credits\":-5}"}) {
            mockMvc.perform(patchAs(user, "/courses/" + courseId).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void createRejectsNegativeCredits() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");

        mockMvc.perform(postAs(user, "/courses",
                        "{\"name\":\"N\",\"code\":\"ABC-100\",\"credits\":-1,\"periodId\":\"%s\"}".formatted(periodId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateCodeOnPutAndPatchIsAConflict() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        createCourse(user, periodId, "MAT-101");
        String other = createCourse(user, periodId, "FIS-102");

        mockMvc.perform(putAs(user, "/courses/" + other, "{\"name\":\"N\",\"code\":\"mat-101\",\"credits\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COURSE_CODE_ALREADY_EXISTS"));
        mockMvc.perform(patchAs(user, "/courses/" + other).contentType("application/json")
                        .content("{\"code\":\"MAT-101\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COURSE_CODE_ALREADY_EXISTS"));
    }

    @Test
    void listFiltersByProfessorCreditsAndSearch() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        createCourseWith(user, periodId, "MAT-101", "Ada Lovelace", 4, "Calculus");
        createCourseWith(user, periodId, "FIS-102", "Isaac Newton", 3, "Physics");
        createCourseWith(user, periodId, "QUI-103", "Marie Curie", 3, "Chemistry");

        mockMvc.perform(getAs(user, "/courses").param("professor", "newton"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].code").value("FIS-102"));
        mockMvc.perform(getAs(user, "/courses").param("credits", "3"))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mockMvc.perform(getAs(user, "/courses").param("search", "chem"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].code").value("QUI-103"));
        mockMvc.perform(getAs(user, "/courses").param("search", "mat-1"))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(getAs(user, "/courses").param("periodId", periodId).param("credits", "3").param("professor", "curie"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void validateCodeReportsFormatAvailabilityAndExcludesTheCourseItself() throws Exception {
        String user = newUserId();
        String periodId = createPeriod(user, "P1");
        String courseId = createCourse(user, periodId, "MAT-101");

        mockMvc.perform(postAs(user, "/courses/validate-code",
                        "{\"code\":\"MAT-101\",\"periodId\":\"%s\"}".formatted(periodId)))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(postAs(user, "/courses/validate-code",
                        "{\"code\":\"MAT-101\",\"periodId\":\"%s\",\"courseId\":\"%s\"}".formatted(periodId, courseId)))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(true));
        mockMvc.perform(postAs(user, "/courses/validate-code",
                        "{\"code\":\"not a code\",\"periodId\":\"%s\"}".formatted(periodId)))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("Course code format is invalid"));
    }

    @Test
    void validateCodeForUnknownOrForeignPeriodIsNotFound() throws Exception {
        String foreignPeriod = createPeriod(newUserId(), "P1");

        mockMvc.perform(postAs(newUserId(), "/courses/validate-code",
                        "{\"code\":\"MAT-101\",\"periodId\":\"%s\"}".formatted(foreignPeriod)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
        mockMvc.perform(postAs(newUserId(), "/courses/validate-code",
                        "{\"code\":\"MAT-101\",\"periodId\":\"missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() throws Exception {
        String user = newUserId();
        createCourse(user, createPeriod(user, "P1"), "MAT-101");

        mockMvc.perform(getAs(user, "/courses").param("search", "%"))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(getAs(user, "/courses").param("search", "_"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
