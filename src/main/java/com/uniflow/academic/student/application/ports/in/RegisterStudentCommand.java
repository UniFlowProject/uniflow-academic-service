package com.uniflow.academic.student.application.ports.in;

import com.uniflow.academic.student.domain.Student;

public interface RegisterStudentCommand {

    Student register(RegisterStudentRequest request);

    record RegisterStudentRequest(String accessToken) {
        public RegisterStudentRequest {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("Access token is required");
            }
        }
    }
}
