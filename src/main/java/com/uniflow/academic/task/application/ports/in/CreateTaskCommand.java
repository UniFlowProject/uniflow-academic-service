package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskDraft;

public interface CreateTaskCommand {

    Task execute(TaskDraft draft, String userId);
}
