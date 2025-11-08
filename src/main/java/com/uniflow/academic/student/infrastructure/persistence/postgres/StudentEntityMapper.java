package com.uniflow.academic.student.infrastructure.persistence.postgres;

import com.uniflow.academic.student.domain.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentEntityMapper {

    public StudentEntity toEntity(Student student) {
        if (student == null) {
            return null;
        }
        return StudentEntity.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .provider(student.getProvider())
                .providerId(student.getProviderId())
                .studentId(student.getStudentId())
                .avatar(student.getAvatar())
                .accessToken(student.getAccessToken())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }

    public Student toDomain(StudentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Student.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .provider(entity.getProvider())
                .providerId(entity.getProviderId())
                .studentId(entity.getStudentId())
                .avatar(entity.getAvatar())
                .accessToken(entity.getAccessToken())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
