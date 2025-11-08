package com.uniflow.academic.student.application.services;

import com.uniflow.academic.shared.infrastructure.security.GoogleTokenValidator;
import com.uniflow.academic.student.application.ports.in.RegisterStudentCommand;
import com.uniflow.academic.student.application.ports.out.StudentRepository;
import com.uniflow.academic.student.domain.Student;
import com.uniflow.academic.student.domain.exception.InvalidStudentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterStudentService implements RegisterStudentCommand {

    private static final String GOOGLE_PROVIDER = "google";

    private final GoogleTokenValidator googleTokenValidator;
    private final StudentRepository studentRepository;

    @Override
    public Student register(RegisterStudentRequest request) {
        log.info("Registering student with Google token");
        Map<String, Object> tokenInfo = googleTokenValidator.validateToken(request.accessToken());
        if (tokenInfo == null) {
            throw new InvalidStudentException("Invalid Google access token");
        }

        String providerId = Optional.ofNullable(tokenInfo.get("sub"))
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new InvalidStudentException("Google token missing subject identifier"));

        String email = Optional.ofNullable(tokenInfo.get("email"))
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new InvalidStudentException("Google token missing email"));

        String name = Optional.ofNullable(tokenInfo.get("name"))
                .map(Object::toString)
                .orElse(email);

        String avatar = Optional.ofNullable(tokenInfo.get("picture"))
                .map(Object::toString)
                .orElse(null);

        Student student = studentRepository.findByProvider(GOOGLE_PROVIDER, providerId)
                .map(existing -> existing.refreshFromProvider(name, email, avatar, request.accessToken()))
                .map(studentRepository::update)
                .orElseGet(() -> studentRepository.save(
                        Student.createFromProvider(
                                GOOGLE_PROVIDER,
                                providerId,
                                name,
                                email,
                                avatar,
                                request.accessToken()
                        )
                ));

        log.info("Student {} registered successfully", student.getId());
        return student;
    }
}
