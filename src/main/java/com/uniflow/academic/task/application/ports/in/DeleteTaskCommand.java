package com.uniflow.academic.task.application.ports.in;

public interface DeleteTaskCommand {

    void execute(String taskId, String userId);
}
