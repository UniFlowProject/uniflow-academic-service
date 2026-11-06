package com.uniflow.academic.shared;

import com.uniflow.academic.shared.application.PaginationParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationParamsTest {

    @Test
    void nullsFallBackToDefaults() {
        PaginationParams params = PaginationParams.of(null, null);

        assertThat(params.page()).isEqualTo(PaginationParams.DEFAULT_PAGE).isEqualTo(1);
        assertThat(params.limit()).isEqualTo(PaginationParams.DEFAULT_LIMIT).isEqualTo(10);
    }

    @Test
    void boundaryValuesAreAccepted() {
        assertThat(PaginationParams.of(1, 1).limit()).isEqualTo(1);
        assertThat(PaginationParams.of(5, PaginationParams.MAX_LIMIT).limit()).isEqualTo(100);
    }

    @Test
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> PaginationParams.of(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaginationParams.of(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaginationParams.of(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaginationParams.of(1, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PaginationParams(1, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultLimitCanBeOverriddenPerResource() {
        assertThat(PaginationParams.of(null, null, 20).limit()).isEqualTo(20);
        assertThat(PaginationParams.of(2, 5, 20).limit()).isEqualTo(5);
        assertThatThrownBy(() -> PaginationParams.of(1, 101, 20)).isInstanceOf(IllegalArgumentException.class);
    }
}
