package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.GetTaskByIdQuery;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTaskByIdService implements GetTaskByIdQuery {

    private final TaskRepository taskRepository;

    @Override
    public Task execute(String taskId, String userId) {
        return taskRepository.requireOwned(taskId, userId);
    }
}
