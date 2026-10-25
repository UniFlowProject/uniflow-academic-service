package com.uniflow.academic.task.infrastructure.web.dto;

final class TaskPatterns {

    static final String STATUS = "todo|in-progress|in-review|done|cancelled";
    static final String PRIORITY = "low|medium|high|urgent";
    static final String TYPE = "assignment|exam|reading|presentation|lab|quiz|essay|group-work";
    static final String HTTP_URL = "(?i)^https?://\\S+$";
    static final int MAX_TITLE = 255;
    static final int MAX_DESCRIPTION = 5000;
    static final int MAX_ID = 36;
    static final int MAX_LIST_SIZE = 50;
    static final int MAX_ELEMENT = 100;
    static final int MAX_ATTACHMENT = 2048;
    static final String NOT_BLANK = ".*\\S.*";

    private TaskPatterns() {
    }
}
