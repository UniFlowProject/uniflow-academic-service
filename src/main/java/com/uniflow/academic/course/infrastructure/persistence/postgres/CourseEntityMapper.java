package com.uniflow.academic.course.infrastructure.persistence.postgres;

import com.uniflow.academic.course.domain.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseEntityMapper {

    public CourseEntity toEntity(Course course) {
        return CourseEntity.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .professor(course.getProfessor())
                .credits(course.getCredits())
                .color(course.getColor())
                .periodId(course.getPeriodId())
                .userId(course.getUserId())
                .description(course.getDescription())
                .schedule(course.getSchedule())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public Course toDomain(CourseEntity entity) {
        return Course.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .professor(entity.getProfessor())
                .credits(entity.getCredits())
                .color(entity.getColor())
                .periodId(entity.getPeriodId())
                .userId(entity.getUserId())
                .description(entity.getDescription())
                .schedule(entity.getSchedule())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
