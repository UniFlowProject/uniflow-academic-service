package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.DeleteTaskCommand;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTaskService implements DeleteTaskCommand {

    private final TaskRepository taskRepository;
    private final TaskReminders reminders;

    @Override
    public void execute(String taskId, String userId) {
        log.info("Deleting task {} for user {}", taskId, userId);

        Task task = taskRepository.requireOwned(taskId, userId);
        if (!taskRepository.delete(task.getId(), userId)) {
            throw new TaskNotFoundException(taskId);
        }
        reminders.cancel(task.getId());
    }
}
