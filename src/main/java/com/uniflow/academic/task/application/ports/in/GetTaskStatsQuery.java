package com.uniflow.academic.task.application.ports.in;

import com.uniflow.academic.task.domain.TaskStats;

public interface GetTaskStatsQuery {

    TaskStats execute(String userId);
}
