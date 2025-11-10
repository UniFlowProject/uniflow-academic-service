package com.uniflow.academic.auth.application.ports.out;

import com.uniflow.academic.student.domain.Student;

public interface GoogleAuthPort {
    Student validateAuthorizationCode(String code, String redirectUri);
}