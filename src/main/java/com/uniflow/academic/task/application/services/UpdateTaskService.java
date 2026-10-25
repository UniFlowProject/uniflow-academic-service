package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.UpdateTaskCommand;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskChanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateTaskService implements UpdateTaskCommand {

    private final TaskRepository taskRepository;
    private final TaskParentsResolver parents;
    private final Clock clock;
    private final TaskReminders reminders;

    @Override
    public Task execute(String taskId, TaskChanges changes, String userId) {
        log.info("Updating task {} for user {}", taskId, userId);

        Task task = taskRepository.requireOwned(taskId, userId);
        task.ensureModifiable();

        Task updated = task.withChanges(withOwnedParents(task, changes, userId), clock.instant());
        taskRepository.save(updated);

        if (!updated.getDueDate().equals(task.getDueDate())) {
            reminders.schedule(updated);
        }
        return updated;
    }

    private TaskChanges withOwnedParents(Task task, TaskChanges changes, String userId) {
        if (changes.periodId() == null && changes.courseId() == null) {
            return changes;
        }
        TaskParentsResolver.Parents resolved = parents.resolve(
                changes.periodId() != null ? changes.periodId() : task.getPeriodId(),
                changes.courseId() != null ? changes.courseId() : task.getCourseId(),
                userId
        );
        return changes.toBuilder().periodId(resolved.periodId()).courseId(resolved.courseId()).build();
    }
}
