package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.task.domain.Task;

public interface GetTaskByIdQuery {

    Task execute(String taskId, String userId);
}
