package com.uniflow.academic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests skip silently without Docker. On CI that would hide every test,
 * so the build must fail there instead of passing with skipped tests.
 */
class DockerAvailabilityTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    void dockerMustBeAvailableOnCi() {
        assertThat(DockerClientFactory.instance().isDockerAvailable())
                .as("Docker is required on CI so the Testcontainers integration tests actually run")
                .isTrue();
    }
}
