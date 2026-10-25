package com.uniflow.academic.task.domain;

/**
 * Enum with a stable lowercase wire representation, shared by the HTTP and persistence layers.
 */
public interface WireEnum {

    String value();

    static <E extends Enum<E> & WireEnum> E parse(Class<E> type, String raw) {
        if (raw != null) {
            for (E constant : type.getEnumConstants()) {
                if (constant.value().equals(raw)) {
                    return constant;
                }
            }
        }
        throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": '" + raw + "'");
    }
}
