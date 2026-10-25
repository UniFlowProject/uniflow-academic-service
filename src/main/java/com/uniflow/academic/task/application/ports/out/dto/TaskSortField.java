package com.uniflow.academic.task.application.ports.out.dto;

import com.uniflow.academic.task.domain.WireEnum;

public enum TaskSortField implements WireEnum {
    DUE_DATE("dueDate"),
    PRIORITY("priority"),
    STATUS("status"),
    CREATED_AT("createdAt");

    private final String value;

    TaskSortField(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static TaskSortField fromValue(String raw) {
        return WireEnum.parse(TaskSortField.class, raw);
    }
}
