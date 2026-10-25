package com.uniflow.academic.shared;

import com.uniflow.academic.shared.domain.exception.DomainException;
import com.uniflow.academic.shared.infrastructure.web.ApiErrorResponse;
import com.uniflow.academic.shared.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static DataIntegrityViolationException integrityViolation(String sqlState) {
        return new DataIntegrityViolationException("boom", new RuntimeException(new SQLException("x", sqlState)));
    }

    @Test
    void uniqueViolationIsAConflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(integrityViolation("23505"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code()).isEqualTo("CONFLICT");
    }

    @Test
    void foreignKeyViolationIsAConflict() {
        assertThat(handler.handleDataIntegrity(integrityViolation("23503")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void notNullViolationIsAnInternalError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(integrityViolation("23502"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void integrityViolationWithoutSqlStateIsAnInternalError() {
        assertThat(handler.handleDataIntegrity(new DataIntegrityViolationException("boom")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void optimisticLockFailureIsATaskConflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleOptimisticLock(
                new org.springframework.orm.ObjectOptimisticLockingFailureException("TaskEntity", "t1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code()).isEqualTo("TASK_CONFLICT");
    }

    @Test
    void jpaOptimisticLockExceptionIsATaskConflictToo() {
        assertThat(handler.handleOptimisticLock(new jakarta.persistence.OptimisticLockException("stale"))
                .getBody().error().code()).isEqualTo("TASK_CONFLICT");
    }

    @Test
    void accessDeniedIsForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().error().message()).doesNotContain("secret");
    }

    @Test
    void unknownDomainExceptionSubclassIsAnInternalError() {
        class UnmappedException extends DomainException {
            UnmappedException() {
                super("UNMAPPED", "unmapped");
            }
        }

        ResponseEntity<ApiErrorResponse> response = handler.handleDomain(new UnmappedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("UNMAPPED");
    }
}
