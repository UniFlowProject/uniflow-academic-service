package com.uniflow.academic.auth.application.ports.in;

import com.uniflow.academic.auth.infrastructure.web.dto.AuthResponse;
import com.uniflow.academic.student.domain.Student;

public interface AuthenticateWithGoogleUseCase {
    AuthResponse execute(String code, String redirectUri);
}