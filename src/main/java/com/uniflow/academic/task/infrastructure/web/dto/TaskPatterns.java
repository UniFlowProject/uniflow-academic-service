package com.uniflow.academic.task.infrastructure.web.dto;

final class TaskPatterns {

    static final String STATUS = "todo|in-progress|in-review|done|cancelled";
    static final String PRIORITY = "low|medium|high|urgent";
    static final String TYPE = "assignment|exam|reading|presentation|lab|quiz|essay|group-work";
    static final String NOT_BLANK = ".*\\S.*";

    private TaskPatterns() {
    }
}
