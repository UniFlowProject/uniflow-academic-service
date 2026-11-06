package com.uniflow.academic.task.domain;

public enum TaskPriority implements WireEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    private final String value;

    TaskPriority(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static TaskPriority fromValue(String raw) {
        return WireEnum.parse(TaskPriority.class, raw);
    }
}
