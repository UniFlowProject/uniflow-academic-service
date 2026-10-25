package com.uniflow.academic.task.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidIdGeneratorTest {

    private final UuidIdGenerator generator = new UuidIdGenerator();

    @Test
    void generatesValidUuidStrings() {
        String id = generator.newId();

        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        assertThat(id).hasSize(36);
    }

    @Test
    void generatesUniqueIds() {
        assertThat(generator.newId()).isNotEqualTo(generator.newId());
    }
}
