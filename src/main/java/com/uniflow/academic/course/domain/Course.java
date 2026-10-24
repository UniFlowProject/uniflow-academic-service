package com.uniflow.academic.course.domain;

import com.uniflow.academic.course.domain.exception.InvalidCourseException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain aggregate representing an academic course.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Course {

    private String id;
    private String name;
    private String code;
    private String professor;
    private Integer credits;
    private String color;
    private String periodId;
    private String userId;
    private String description;
    private List<String> schedule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Validate the course invariants.
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new InvalidCourseException("Course name is required");
        }
        if (code == null || code.isBlank()) {
            throw new InvalidCourseException("Course code is required");
        }
        if (credits == null || credits < 0) {
            throw new InvalidCourseException("Course credits must be zero or positive");
        }
        if (periodId == null || periodId.isBlank()) {
            throw new InvalidCourseException("Course period is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new InvalidCourseException("Course user is required");
        }
    }

    /**
     * Factory method to create a new course ensuring a unique identifier and timestamps.
     */
    public static Course create(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String periodId,
            String userId,
            String description,
            List<String> schedule
    ) {
        Course course = Course.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .code(code)
                .professor(professor)
                .credits(credits)
                .color(color)
                .periodId(periodId)
                .userId(userId)
                .description(description)
                .schedule(schedule)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        course.validate();
        return course;
    }

    /**
     * Update the course with the provided information.
     */
    public Course update(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description,
            List<String> schedule
    ) {
        Course updatedCourse = this.toBuilder()
                .name(name)
                .code(code)
                .professor(professor)
                .credits(credits)
                .color(color)
                .description(description)
                .schedule(schedule)
                .updatedAt(LocalDateTime.now())
                .build();

        updatedCourse.validate();
        return updatedCourse;
    }

    /**
     * Update the course with the provided information.
     */
    public Course patch(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description
    ) {
        Course.CourseBuilder builder = this.toBuilder();

        if (name != null) builder.name(name);
        if (code != null) builder.code(code);
        if (professor != null) builder.professor(professor);
        if (credits != null) builder.credits(credits);
        if (color != null) builder.color(color);
        if (description != null) builder.description(description);

        Course updatedCourse = builder
                .updatedAt(LocalDateTime.now())
                .build();

        updatedCourse.validate();
        return updatedCourse;
    }
}
