package com.uniflow.academic.task.application.services;

import com.uniflow.academic.task.application.ports.in.GetTaskStatsQuery;
import com.uniflow.academic.task.application.ports.out.TaskRepository;
import com.uniflow.academic.task.domain.TaskStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTaskStatsService implements GetTaskStatsQuery {

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Override
    public TaskStats execute(String userId) {
        return TaskStats.from(taskRepository.findAllByUserId(userId), clock.instant());
    }
}
