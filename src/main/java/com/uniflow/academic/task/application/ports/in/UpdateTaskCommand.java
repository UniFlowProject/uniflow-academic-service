package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskChanges;

public interface UpdateTaskCommand {

    Task execute(String taskId, TaskChanges changes, String userId);
}
