package com.uniflow.academic.shared.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, List<Detail> details) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(String field, String message) {
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ErrorBody(code, message, null));
    }

    public static ApiErrorResponse of(String code, String message, List<Detail> details) {
        return new ApiErrorResponse(new ErrorBody(code, message, details));
    }
}
