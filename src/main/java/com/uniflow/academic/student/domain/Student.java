package com.uniflow.academic.student.domain;

import com.uniflow.academic.student.domain.exception.InvalidStudentException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain aggregate representing a student authenticated via an OAuth provider.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Student {

    private String id;
    private String name;
    private String email;
    private String provider;
    private String providerId;
    private String studentId;
    private String avatar;
    private String accessToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Validates the aggregate invariants.
     */
    public void validate() {
        if (email == null || email.isBlank()) {
            throw new InvalidStudentException("Student email is required");
        }
        if (provider == null || provider.isBlank()) {
            throw new InvalidStudentException("Authentication provider is required");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new InvalidStudentException("Provider identifier is required");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidStudentException("OAuth access token is required");
        }
    }

    /**
     * Factory method that creates a new student from OAuth provider data.
     */
    public static Student createFromProvider(
            String provider,
            String providerId,
            String name,
            String email,
            String avatar,
            String accessToken
    ) {
        Student student = Student.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .avatar(avatar)
                .accessToken(accessToken)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        student.validate();
        return student;
    }

    /**
     * Updates the student information using the latest OAuth profile data.
     */
    public Student refreshFromProvider(
            String name,
            String email,
            String avatar,
            String accessToken
    ) {
        Student updated = this.toBuilder()
                .name(name)
                .email(email)
                .avatar(avatar)
                .accessToken(accessToken)
                .updatedAt(LocalDateTime.now())
                .build();
        updated.validate();
        return updated;
    }

    /**
     * Assigns a university student identifier.
     */
    public Student assignStudentId(String studentId) {
        Student updated = this.toBuilder()
                .studentId(studentId)
                .updatedAt(LocalDateTime.now())
                .build();
        updated.validate();
        return updated;
    }
}
