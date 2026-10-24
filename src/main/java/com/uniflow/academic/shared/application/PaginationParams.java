package com.uniflow.academic.shared.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for pagination parameters.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationParams {
    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer limit = 10;

    public void validate() {
        if (page == null || page < 1) {
            this.page = 1;
        }
        if (limit == null || limit < 1 || limit > 100) {
            this.limit = 10;
        }
    }

    public Integer getOffset() {
        validate();
        return (page - 1) * limit;
    }
}
