package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.ChangeTaskStatusCommand;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeTaskStatusService implements ChangeTaskStatusCommand {

    private final TaskRepository taskRepository;
    private final Clock clock;
    private final TaskReminders reminders;

    @Override
    public Task execute(String taskId, TaskStatus status, String userId) {
        log.info("Moving task {} to {} for user {}", taskId, status, userId);

        Task transitioned = taskRepository.requireOwned(taskId, userId).transitionTo(status, clock.instant());
        Task saved = taskRepository.save(transitioned);

        if (!saved.canBeModified()) {
            reminders.cancel(saved.getId());
        }
        return saved;
    }
}
