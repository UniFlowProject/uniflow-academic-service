package com.uniflow.academic.course.infrastructure.persistence.postgres;

import com.uniflow.academic.course.infrastructure.persistence.postgres.converter.ListToJsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "courses",
        schema = "academic",
        indexes = {
                @Index(name = "idx_course_user", columnList = "user_id"),
                @Index(name = "idx_course_period", columnList = "period_id"),
                @Index(name = "idx_course_professor", columnList = "professor")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {})
public class CourseEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "professor")
    private String professor;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "color")
    private String color;

    @Column(name = "period_id", nullable = false)
    private String periodId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Convert(converter = ListToJsonConverter.class)
    @Column(name = "schedule", columnDefinition = "TEXT")
    private List<String> schedule;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
