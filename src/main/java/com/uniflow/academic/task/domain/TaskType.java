package com.uniflow.academic.task.domain;

public enum TaskType implements WireEnum {
    ASSIGNMENT("assignment"),
    EXAM("exam"),
    READING("reading"),
    PRESENTATION("presentation"),
    LAB("lab"),
    QUIZ("quiz"),
    ESSAY("essay"),
    GROUP_WORK("group-work");

    private final String value;

    TaskType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static TaskType fromValue(String raw) {
        return WireEnum.parse(TaskType.class, raw);
    }
}
