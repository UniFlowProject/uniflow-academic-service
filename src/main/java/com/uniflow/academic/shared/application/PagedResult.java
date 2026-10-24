package com.uniflow.academic.shared.application;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(List<T> items, PageInfo pageInfo) {

    public record PageInfo(
            long total,
            int page,
            int limit,
            int totalPages,
            boolean hasNext,
            boolean hasPrev
    ) {
    }

    public static <T> PagedResult<T> of(List<T> items, long total, int page, int limit) {
        int totalPages = (int) Math.ceil((double) total / limit);
        return new PagedResult<>(
                items,
                new PageInfo(total, page, limit, totalPages, page < totalPages, page > 1)
        );
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(items.stream().map(mapper).toList(), pageInfo);
    }
}
