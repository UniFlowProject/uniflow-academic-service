package com.uniflow.academic.student.infrastructure.web.dto.mapper;

import com.uniflow.academic.student.application.ports.in.RegisterStudentCommand;
import com.uniflow.academic.student.domain.Student;
import com.uniflow.academic.student.infrastructure.web.dto.RegisterStudentHttpRequest;
import com.uniflow.academic.student.infrastructure.web.dto.StudentHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentHttpMapper {

    public RegisterStudentCommand.RegisterStudentRequest toRegisterCommand(RegisterStudentHttpRequest request) {
        return new RegisterStudentCommand.RegisterStudentRequest(request.getAccessToken());
    }

    public StudentHttpResponse toHttpResponse(Student student) {
        if (student == null) {
            return null;
        }
        return StudentHttpResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .provider(student.getProvider())
                .providerId(student.getProviderId())
                .studentId(student.getStudentId())
                .avatar(student.getAvatar())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
