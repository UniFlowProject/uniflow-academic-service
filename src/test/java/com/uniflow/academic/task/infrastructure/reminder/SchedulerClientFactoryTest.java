package com.uniflow.academic.task.infrastructure.reminder;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulerClientFactoryTest {

    private static ReminderProperties properties(Duration apiTimeout, Duration attemptTimeout, int retries,
                                                 Duration connectTimeout, Duration socketTimeout) {
        return new ReminderProperties(ReminderProperties.Scheduler.NOOP, "default", null, null, 24,
                apiTimeout, attemptTimeout, retries, connectTimeout, socketTimeout);
    }

    private static SchedulerClient build(ReminderProperties properties, URI endpoint) {
        return SchedulerClientFactory.configuredBuilder(properties)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(endpoint)
                .build();
    }

    @Test
    void clientCarriesTheConfiguredTimeoutsAndRetryPolicy() {
        ReminderProperties properties = properties(
                Duration.ofSeconds(7), Duration.ofSeconds(3), 4, Duration.ofSeconds(1), Duration.ofSeconds(2));

        try (SchedulerClient client = build(properties, URI.create("http://localhost:1"))) {
            var override = client.serviceClientConfiguration().overrideConfiguration();

            assertThat(override.apiCallTimeout()).contains(Duration.ofSeconds(7));
            assertThat(override.apiCallAttemptTimeout()).contains(Duration.ofSeconds(3));
            assertThat(override.retryStrategy().orElseThrow().maxAttempts()).isEqualTo(5);
        }
    }

    @Test
    void zeroRetriesMeansASingleAttempt() {
        ReminderProperties properties = properties(
                Duration.ofSeconds(5), Duration.ofSeconds(2), 0, Duration.ofSeconds(1), Duration.ofSeconds(2));

        try (SchedulerClient client = build(properties, URI.create("http://localhost:1"))) {
            assertThat(client.serviceClientConfiguration().overrideConfiguration()
                    .retryStrategy().orElseThrow().maxAttempts()).isEqualTo(1);
        }
    }

    @Test
    void aServerThatNeverAnswersIsGivenUpOnAfterTheSocketTimeout() throws Exception {
        try (ServerSocket silent = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ReminderProperties properties = properties(
                    Duration.ofSeconds(30), Duration.ofSeconds(30), 0, Duration.ofSeconds(1), Duration.ofMillis(300));
            URI endpoint = URI.create("http://127.0.0.1:" + silent.getLocalPort());

            try (SchedulerClient client = build(properties, endpoint)) {
                long started = System.nanoTime();

                assertThatThrownBy(() -> client.deleteSchedule(
                        DeleteScheduleRequest.builder().name("n").groupName("g").build()))
                        .isInstanceOf(SdkClientException.class);

                assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
            }
        }
    }

    @Test
    void theWholeCallIsBoundedByTheApiCallTimeout() throws Exception {
        try (ServerSocket silent = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ReminderProperties properties = properties(
                    Duration.ofMillis(400), Duration.ofMillis(400), 2, Duration.ofSeconds(1), Duration.ofSeconds(30));
            URI endpoint = URI.create("http://127.0.0.1:" + silent.getLocalPort());

            try (SchedulerClient client = build(properties, endpoint)) {
                long started = System.nanoTime();

                assertThatThrownBy(() -> client.deleteSchedule(
                        DeleteScheduleRequest.builder().name("n").groupName("g").build()))
                        .isInstanceOf(SdkClientException.class);

                assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
            }
        }
    }
}
