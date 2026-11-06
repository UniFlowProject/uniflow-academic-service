package com.uniflow.academic.task.infrastructure.reminder;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.SchedulerClientBuilder;

/**
 * Builds the EventBridge Scheduler client with explicit timeouts and a small retry budget, so a slow
 * or unreachable AWS endpoint cannot hold a reminder thread for long. Region and credentials are left
 * to the AWS default provider chains unless the caller sets them.
 */
final class SchedulerClientFactory {

    private SchedulerClientFactory() {
    }

    static SchedulerClientBuilder configuredBuilder(ReminderProperties properties) {
        return SchedulerClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(properties.apiTimeout())
                        .apiCallAttemptTimeout(properties.apiAttemptTimeout())
                        .retryStrategy(AwsRetryStrategy.standardRetryStrategy().toBuilder()
                                .maxAttempts(properties.maxRetries() + 1)
                                .build())
                        .build())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(properties.connectTimeout())
                        .socketTimeout(properties.socketTimeout()));
    }
}
