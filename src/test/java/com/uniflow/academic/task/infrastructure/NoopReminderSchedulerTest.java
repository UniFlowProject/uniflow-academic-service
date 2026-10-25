package com.uniflow.academic.task.infrastructure;

import com.uniflow.academic.task.infrastructure.reminder.NoopReminderScheduler;
import org.junit.jupiter.api.Test;

import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThatCode;

class NoopReminderSchedulerTest {

    private final NoopReminderScheduler scheduler = new NoopReminderScheduler();

    @Test
    void scheduleAndCancelDoNothingAndNeverFail() {
        assertThatCode(() -> {
            scheduler.schedule(aTask().build());
            scheduler.cancel("t1");
            scheduler.cancel("unknown");
        }).doesNotThrowAnyException();
    }
}
