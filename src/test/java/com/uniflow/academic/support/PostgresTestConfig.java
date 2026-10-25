package com.uniflow.academic.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared test infrastructure: a single Postgres container (wired through @ServiceConnection)
 * a mocked JwtDecoder so no identity provider is contacted, and a recording reminder scheduler.
 * Authenticated requests use spring-security-test's jwt() post processor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    @Bean
    @Primary
    RecordingReminderScheduler recordingReminderScheduler() {
        return new RecordingReminderScheduler();
    }
}
