package com.uniflow.academic.shared.infrastructure.web;

import com.uniflow.academic.shared.domain.exception.ConflictException;
import com.uniflow.academic.shared.domain.exception.DomainException;
import com.uniflow.academic.shared.domain.exception.InvalidDomainException;
import com.uniflow.academic.shared.domain.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String FORBIDDEN = "FORBIDDEN";

    private static final String VALIDATION_FAILED_MESSAGE = "Request validation failed";
    private static final String SQL_STATE_UNIQUE_VIOLATION = "23505";
    private static final String SQL_STATE_FOREIGN_KEY_VIOLATION = "23503";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = statusOf(ex);
        log.warn("{} ({}): {}", ex.getClass().getSimpleName(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(status).body(ApiErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiErrorResponse.Detail> details = ex.getConstraintViolations().stream()
                .map(v -> new ApiErrorResponse.Detail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(VALIDATION_ERROR, VALIDATION_FAILED_MESSAGE, details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                VALIDATION_ERROR,
                VALIDATION_FAILED_MESSAGE,
                List.of(new ApiErrorResponse.Detail(ex.getName(), "Invalid value"))
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String sqlState = sqlStateOf(ex);
        if (SQL_STATE_UNIQUE_VIOLATION.equals(sqlState) || SQL_STATE_FOREIGN_KEY_VIOLATION.equals(sqlState)) {
            log.warn("Integrity conflict (SQLState {}): {}", sqlState, ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(CONFLICT, "The operation conflicts with existing data"));
        }
        log.error("Unexpected data integrity error (SQLState {})", sqlState, ex);
        return internalError();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(FORBIDDEN, "Access is denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return internalError();
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ApiErrorResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiErrorResponse.Detail(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(VALIDATION_ERROR, VALIDATION_FAILED_MESSAGE, details));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(MALFORMED_REQUEST, "The request body could not be parsed"));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String code = status.name();
        String message = status.is5xxServerError() ? status.getReasonPhrase() : ex.getMessage();
        return ResponseEntity.status(statusCode).headers(headers).body(ApiErrorResponse.of(code, message));
    }

    private ResponseEntity<ApiErrorResponse> internalError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(INTERNAL_ERROR, "An unexpected error occurred"));
    }

    private static String sqlStateOf(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }

    private HttpStatus statusOf(DomainException ex) {
        if (ex instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (ex instanceof InvalidDomainException) {
            return HttpStatus.BAD_REQUEST;
        }
        log.warn("No HTTP status mapped for {}; defaulting to 500", ex.getClass().getName());
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
