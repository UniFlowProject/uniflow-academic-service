package com.uniflow.academic.task.domain;

public enum TaskStatus implements WireEnum {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    IN_REVIEW("in-review"),
    DONE("done"),
    CANCELLED("cancelled");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static TaskStatus fromValue(String raw) {
        return WireEnum.parse(TaskStatus.class, raw);
    }
}
