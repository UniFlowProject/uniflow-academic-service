package com.uniflow.academic.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RecordingReminderScheduler reminders;

    @BeforeEach
    void resetReminders() {
        reminders.reset();
    }

    protected static String newUserId() {
        return UUID.randomUUID().toString();
    }

    protected static RequestPostProcessor asUser(String userId) {
        return jwt().jwt(token -> token.subject(userId));
    }

    protected MockHttpServletRequestBuilder getAs(String userId, String path) {
        return get(path).with(asUser(userId));
    }

    protected MockHttpServletRequestBuilder deleteAs(String userId, String path) {
        return delete(path).with(asUser(userId));
    }

    protected MockHttpServletRequestBuilder patchAs(String userId, String path) {
        return patch(path).with(asUser(userId));
    }

    protected MockHttpServletRequestBuilder postAs(String userId, String path, String json) {
        return post(path).with(asUser(userId)).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected MockHttpServletRequestBuilder putAs(String userId, String path, String json) {
        return put(path).with(asUser(userId)).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    protected String createPeriod(String userId, String name) throws Exception {
        String body = mockMvc.perform(postAs(userId, "/periods", """
                        {"name":"%s","type":"first-semester","year":2025,
                         "startDate":"2025-02-03","endDate":"2025-06-20"}
                        """.formatted(name)))
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }

    protected String createCourse(String userId, String periodId, String code) throws Exception {
        String body = mockMvc.perform(postAs(userId, "/courses", courseJson(periodId, code)))
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }

    protected String courseJson(String periodId, String code) {
        return """
                {"name":"Calculus","code":"%s","credits":4,"periodId":"%s"}
                """.formatted(code, periodId);
    }
}
