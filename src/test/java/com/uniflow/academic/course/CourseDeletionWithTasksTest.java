package com.uniflow.academic.course;

import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.application.ports.out.CourseTaskLookup;
import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseDeletionWithTasksTest extends IntegrationTestBase {

    @Autowired
    private CourseTaskLookup courseTaskLookup;

    @Autowired
    private CourseRepository courseRepository;

    private String user;
    private String periodId;
    private String courseId;

    @BeforeEach
    void setUp() throws Exception {
        user = newUserId();
        periodId = createPeriod(user, "P1");
        courseId = createCourse(user, periodId, "MAT-101");
    }

    private String createTask() throws Exception {
        String body = mockMvc.perform(postAs(user, "/tasks", """
                        {"title":"Essay","courseId":"%s","periodId":"%s","dueDate":"2099-11-01T10:00:00Z",
                         "priority":"high","type":"essay","estimatedTimeHours":4}
                        """.formatted(courseId, periodId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }

    @Test
    void courseWithTasksCannotBeDeleted() throws Exception {
        createTask();

        mockMvc.perform(deleteAs(user, "/courses/" + courseId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COURSE_HAS_TASKS"));
        mockMvc.perform(getAs(user, "/courses/" + courseId)).andExpect(status().isOk());
    }

    @Test
    void courseCanBeDeletedOnceItsTasksAreGone() throws Exception {
        String taskId = createTask();
        mockMvc.perform(deleteAs(user, "/courses/" + courseId)).andExpect(status().isConflict());

        mockMvc.perform(deleteAs(user, "/tasks/" + taskId)).andExpect(status().isNoContent());

        mockMvc.perform(deleteAs(user, "/courses/" + courseId)).andExpect(status().isNoContent());
        mockMvc.perform(getAs(user, "/courses/" + courseId)).andExpect(status().isNotFound());
    }

    @Test
    void closedTasksAlsoBlockCourseDeletion() throws Exception {
        String taskId = createTask();
        mockMvc.perform(patchAs(user, "/tasks/" + taskId + "/status").contentType("application/json")
                .content("{\"status\":\"cancelled\"}")).andExpect(status().isOk());

        mockMvc.perform(deleteAs(user, "/courses/" + courseId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COURSE_HAS_TASKS"));
    }

    @Test
    void anotherUsersCourseStaysNotFoundEvenIfItHasTasks() throws Exception {
        createTask();

        mockMvc.perform(deleteAs(newUserId(), "/courses/" + courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void lookupReportsTasksPerUserAndCourse() throws Exception {
        assertThat(courseTaskLookup.existsByCourseId(courseId, user)).isFalse();

        createTask();

        assertThat(courseTaskLookup.existsByCourseId(courseId, user)).isTrue();
        assertThat(courseTaskLookup.existsByCourseId(courseId, newUserId())).isFalse();
        assertThat(courseTaskLookup.existsByCourseId("missing", user)).isFalse();
    }

    @Test
    void foreignKeyStopsADeleteThatSlipsPastTheCheck() throws Exception {
        createTask();

        assertThatThrownBy(() -> courseRepository.delete(courseId, user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
