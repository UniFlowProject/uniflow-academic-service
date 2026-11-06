package com.uniflow.academic.task.application.ports.out.dto;

import com.uniflow.academic.task.domain.WireEnum;

public enum SortOrder implements WireEnum {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static SortOrder fromValue(String raw) {
        return WireEnum.parse(SortOrder.class, raw);
    }
}
