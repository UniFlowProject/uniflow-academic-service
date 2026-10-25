package com.uniflow.academic.shared;

import com.uniflow.academic.shared.application.PagedResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagedResultTest {

    @Test
    void middlePageHasNextAndPrevious() {
        PagedResult.PageInfo info = PagedResult.of(List.of(), 45, 2, 20).pageInfo();

        assertThat(info.totalPages()).isEqualTo(3);
        assertThat(info.hasNext()).isTrue();
        assertThat(info.hasPrev()).isTrue();
    }

    @Test
    void emptyResultHasNoPages() {
        PagedResult.PageInfo info = PagedResult.of(List.of(), 0, 1, 20).pageInfo();

        assertThat(info.totalPages()).isZero();
        assertThat(info.hasNext()).isFalse();
        assertThat(info.hasPrev()).isFalse();
    }
}
