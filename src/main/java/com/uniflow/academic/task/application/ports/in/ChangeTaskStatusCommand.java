package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskStatus;

public interface ChangeTaskStatusCommand {

    Task execute(String taskId, TaskStatus status, String userId);
}
