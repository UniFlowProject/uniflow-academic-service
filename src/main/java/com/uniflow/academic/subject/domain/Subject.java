package com.uniflow.academic.subject.domain;

import com.uniflow.academic.subject.domain.exception.InvalidSubjectException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain aggregate representing an academic subject.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Subject {

    private String id;
    private String name;
    private String code;
    private String professor;
    private Integer credits;
    private String color;
    private String periodId;
    private String studentId;
    private String description;
    private List<String> schedule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Validate the subject invariants.
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new InvalidSubjectException("Subject name is required");
        }
        if (code == null || code.isBlank()) {
            throw new InvalidSubjectException("Subject code is required");
        }
        if (credits == null || credits < 0) {
            throw new InvalidSubjectException("Subject credits must be zero or positive");
        }
        if (periodId == null || periodId.isBlank()) {
            throw new InvalidSubjectException("Subject period is required");
        }
        if (studentId == null || studentId.isBlank()) {
            throw new InvalidSubjectException("Subject student is required");
        }
    }

    /**
     * Factory method to create a new subject ensuring a unique identifier and timestamps.
     */
    public static Subject create(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String periodId,
            String studentId,
            String description,
            List<String> schedule
    ) {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .code(code)
                .professor(professor)
                .credits(credits)
                .color(color)
                .periodId(periodId)
                .studentId(studentId)
                .description(description)
                .schedule(schedule)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        subject.validate();
        return subject;
    }

    /**
     * Update the subject with the provided information.
     */
    public Subject update(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description,
            List<String> schedule
    ) {
        Subject updatedSubject = this.toBuilder()
                .name(name)
                .code(code)
                .professor(professor)
                .credits(credits)
                .color(color)
                .description(description)
                .schedule(schedule)
                .updatedAt(LocalDateTime.now())
                .build();

        updatedSubject.validate();
        return updatedSubject;
    }

    /**
     * Update the subject with the provided information.
     */
    public Subject patch(
            Optional<String> name,
            Optional<String> code,
            Optional<String> professor,
            Optional<Integer> credits,
            Optional<String> color,
            Optional<String> description
    ) {
        Subject.SubjectBuilder builder = this.toBuilder();

        if (name.isPresent()) builder.name(String.valueOf(name));
        if (code.isPresent()) builder.code(String.valueOf(code));
        if (professor.isPresent()) builder.professor(String.valueOf(professor));
        if (credits.isPresent()) builder.credits(Integer.valueOf(String.valueOf(credits)));
        if (color.isPresent()) builder.color(String.valueOf(color));
        if (description.isPresent()) builder.description(String.valueOf(description));

        Subject updatedSubject = builder
                .updatedAt(LocalDateTime.now())
                .build();

        updatedSubject.validate();
        return updatedSubject;
    }
}
