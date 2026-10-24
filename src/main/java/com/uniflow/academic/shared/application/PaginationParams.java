package com.uniflow.academic.shared.application;

/**
 * Validated, immutable pagination request (1-based page). Invalid values are rejected, not corrected.
 */
public record PaginationParams(int page, int limit) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 100;

    public PaginationParams {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    public static PaginationParams of(Integer page, Integer limit) {
        return new PaginationParams(
                page == null ? DEFAULT_PAGE : page,
                limit == null ? DEFAULT_LIMIT : limit
        );
    }

    public static PaginationParams defaults() {
        return new PaginationParams(DEFAULT_PAGE, DEFAULT_LIMIT);
    }
}
