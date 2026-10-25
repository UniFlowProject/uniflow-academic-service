package com.uniflow.academic.task.application;

import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.task.application.services.TaskParentsResolver;
import com.uniflow.academic.task.application.services.TaskReminders;
import com.uniflow.academic.task.domain.Task;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class TaskServiceTestBase {

    static final String USER = "u1";
    static final String OTHER_USER = "u2";

    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    final InMemoryTaskRepository repository = new InMemoryTaskRepository();
    final RecordingReminderScheduler scheduler = new RecordingReminderScheduler();
    final TaskReminders reminders = new TaskReminders(scheduler);
    final PeriodRepository periodRepository = mock(PeriodRepository.class, CALLS_REAL_METHODS);
    final CourseRepository courseRepository = mock(CourseRepository.class, CALLS_REAL_METHODS);
    final TaskParentsResolver parents = new TaskParentsResolver(periodRepository, courseRepository);
    private final AtomicInteger sequence = new AtomicInteger();

    @BeforeEach
    void seedParents() {
        ownedPeriod("p1");
        ownedCourse("c1", "p1");
        repository.save(aTask().build());
    }

    String nextId() {
        return "id-" + sequence.incrementAndGet();
    }

    void ownedPeriod(String periodId) {
        when(periodRepository.findById(periodId, USER))
                .thenReturn(java.util.Optional.of(Period.builder().id(periodId).userId(USER).build()));
    }

    void ownedCourse(String courseId, String periodId) {
        when(courseRepository.findById(courseId, USER)).thenReturn(java.util.Optional.of(
                Course.builder().id(courseId).periodId(periodId).userId(USER).build()));
    }

    Task stored(String taskId) {
        return repository.tasks.get(taskId);
    }
}
