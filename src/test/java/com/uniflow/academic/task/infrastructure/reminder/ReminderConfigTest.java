package com.uniflow.academic.task.infrastructure.reminder;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReminderConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(SchedulerClient.class, () -> mock(SchedulerClient.class))
            .withUserConfiguration(ReminderConfig.class);

    @Test
    void usesTheNoopSchedulerByDefault() {
        runner.run(context -> assertThat(context.getBean(ReminderScheduler.class))
                .isInstanceOf(NoopReminderScheduler.class));
    }

    @Test
    void usesTheNoopSchedulerWhenExplicitlyConfigured() {
        runner.withPropertyValues("reminder.scheduler=noop").run(context ->
                assertThat(context.getBean(ReminderScheduler.class)).isInstanceOf(NoopReminderScheduler.class));
    }

    @Test
    void buildsTheEventBridgeSchedulerWhenConfigured() {
        runner.withPropertyValues(
                "reminder.scheduler=eventbridge",
                "reminder.queue-arn=arn:aws:sqs:us-east-1:123456789012:reminders",
                "reminder.role-arn=arn:aws:iam::123456789012:role/scheduler"
        ).run(context -> {
            assertThat(context).hasSingleBean(ReminderScheduler.class);
            assertThat(context.getBean(ReminderScheduler.class)).isInstanceOf(EventBridgeReminderScheduler.class);
        });
    }

    @Test
    void startupFailsFastWhenEventBridgeIsSelectedWithoutArns() {
        runner.withPropertyValues("reminder.scheduler=eventbridge")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void remindersRunOnABoundedExecutorThatShutsDownWithTheContext() {
        ThreadPoolTaskExecutor[] captured = new ThreadPoolTaskExecutor[1];
        runner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean("reminderExecutor", ThreadPoolTaskExecutor.class);
            captured[0] = executor;
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(4);
            assertThat(executor.getQueueCapacity()).isEqualTo(100);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.AbortPolicy.class);
            assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
        });
        assertThat(captured[0].getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    void asyncExecutionCanBeSwitchedOffForTests() {
        runner.withPropertyValues("reminder.async=false").run(context -> {
            java.util.concurrent.Executor executor = context.getBean("reminderExecutor", java.util.concurrent.Executor.class);
            Thread caller = Thread.currentThread();
            Thread[] ranOn = new Thread[1];

            executor.execute(() -> ranOn[0] = Thread.currentThread());

            assertThat(ranOn[0]).isSameAs(caller);
        });
    }

    @Test
    void sdkClientBuildsWithTheHttpClientOnTheClasspath() {
        SchedulerClient built = SchedulerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        assertThat(built).isNotNull();
        built.close();
    }
}
