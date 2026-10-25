package com.uniflow.academic.task.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(
        name = "tasks",
        schema = "academic",
        indexes = {
                @Index(name = "idx_task_user_due_date", columnList = "user_id, due_date"),
                @Index(name = "idx_task_user_period", columnList = "user_id, period_id"),
                @Index(name = "idx_task_user_course", columnList = "user_id, course_id"),
                @Index(name = "idx_task_course", columnList = "course_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "period_id", nullable = false)
    private String periodId;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "estimated_time_hours", nullable = false)
    private Integer estimatedTimeHours;

    @Column(name = "actual_time_hours")
    private Integer actualTimeHours;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", nullable = false, columnDefinition = "text[]")
    private List<String> tags;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "group_members", nullable = false, columnDefinition = "text[]")
    private List<String> groupMembers;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "attachments", nullable = false, columnDefinition = "text[]")
    private List<String> attachments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
