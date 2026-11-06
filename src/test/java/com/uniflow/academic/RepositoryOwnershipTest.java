package com.uniflow.academic;

import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.exception.CourseNotFoundException;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryOwnershipTest extends IntegrationTestBase {

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private CourseRepository courseRepository;

    private String createCourse(String user, String periodId) throws Exception {
        String body = mockMvc.perform(postAs(user, "/courses", courseJson(periodId, "MAT-101")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }

    @Test
    void periodDeleteIsScopedToTheOwner() throws Exception {
        String owner = newUserId();
        String periodId = createPeriod(owner, "P1");

        assertThat(periodRepository.delete(periodId, newUserId())).isFalse();
        assertThat(periodRepository.existsByIdAndUserId(periodId, owner)).isTrue();
        assertThat(periodRepository.delete(periodId, owner)).isTrue();
        assertThat(periodRepository.existsByIdAndUserId(periodId, owner)).isFalse();
    }

    @Test
    void courseDeleteIsScopedToTheOwner() throws Exception {
        String owner = newUserId();
        String courseId = createCourse(owner, createPeriod(owner, "P1"));

        assertThat(courseRepository.delete(courseId, newUserId())).isFalse();
        assertThat(courseRepository.findById(courseId, owner)).isPresent();
        assertThat(courseRepository.delete(courseId, owner)).isTrue();
    }

    @Test
    void requireOwnedThrowsNotFoundForForeignOrBlankIds() throws Exception {
        String owner = newUserId();
        String periodId = createPeriod(owner, "P1");
        String courseId = createCourse(owner, periodId);

        assertThat(periodRepository.requireOwned(" " + periodId + " ", owner).getId()).isEqualTo(periodId);
        assertThat(courseRepository.requireOwned(courseId, owner).getId()).isEqualTo(courseId);
        assertThatThrownBy(() -> periodRepository.requireOwned(periodId, newUserId()))
                .isInstanceOf(PeriodNotFoundException.class);
        assertThatThrownBy(() -> periodRepository.requireOwned(" ", owner))
                .isInstanceOf(PeriodNotFoundException.class);
        assertThatThrownBy(() -> periodRepository.requireOwned(null, owner))
                .isInstanceOf(PeriodNotFoundException.class);
        assertThatThrownBy(() -> courseRepository.requireOwned(courseId, newUserId()))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void existsByPeriodIdIsScopedToTheOwner() throws Exception {
        String owner = newUserId();
        String periodId = createPeriod(owner, "P1");
        createCourse(owner, periodId);

        assertThat(courseRepository.existsByPeriodId(periodId, owner)).isTrue();
        assertThat(courseRepository.existsByPeriodId(periodId, newUserId())).isFalse();
    }
}
