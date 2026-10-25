package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.CreateTaskCommand;
import com.uniflow.academic.task.application.ports.out.IdGenerator;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateTaskService implements CreateTaskCommand {

    private final TaskRepository taskRepository;
    private final TaskParentsResolver parents;
    private final IdGenerator ids;
    private final Clock clock;
    private final TaskReminders reminders;

    @Override
    public Task execute(TaskDraft draft, String userId) {
        log.info("Creating task for user {}", userId);

        TaskParentsResolver.Parents resolved = parents.resolve(draft.periodId(), draft.courseId(), userId);
        TaskDraft owned = new TaskDraft(
                draft.title(), draft.description(), resolved.courseId(), resolved.periodId(), draft.dueDate(),
                draft.priority(), draft.type(), draft.estimatedTimeHours(),
                draft.tags(), draft.groupMembers(), draft.attachments()
        );

        Task saved = taskRepository.save(Task.create(ids.newId(), userId, owned, clock.instant()));
        reminders.schedule(saved);
        log.info("Task {} created", saved.getId());
        return saved;
    }
}
