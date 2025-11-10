package com.uniflow.academic.auth.application.services;

import com.uniflow.academic.auth.application.ports.in.AuthenticateWithGoogleUseCase;
import com.uniflow.academic.auth.application.ports.out.GoogleAuthPort;
import com.uniflow.academic.auth.application.ports.out.JwtTokenPort;
import com.uniflow.academic.auth.infrastructure.web.dto.AuthResponse;
import com.uniflow.academic.student.application.ports.in.RegisterStudentCommand;
import com.uniflow.academic.student.application.ports.in.GetStudentByProviderIdQuery;
import com.uniflow.academic.student.domain.Student;
import com.uniflow.academic.student.domain.exception.StudentNotFoundException;
import com.uniflow.academic.student.infrastructure.web.dto.mapper.StudentHttpMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticateWithGoogleUseCase {

    private final GoogleAuthPort googleAuthPort;
    private final JwtTokenPort jwtTokenPort;
    private final GetStudentByProviderIdQuery getStudentByProviderIdQuery;
    private final RegisterStudentCommand createStudentCommand;
    private final StudentHttpMapper  studentHttpMapper;

    @Override
//    @Transactional
    public AuthResponse execute(String code, String redirectUri) {
        System.out.println(this.getClass().getSimpleName());
        // Validate Google authorization code
        Student googleUser = googleAuthPort.validateAuthorizationCode(code, redirectUri);

        // Find or create student
        Student student;
        try {
            student = getStudentByProviderIdQuery.execute(googleUser.getProviderId()); // .orElseGet(() -> createStudentCommand.execute(googleUser));
        } catch (StudentNotFoundException e) {
            student = createStudentCommand.register(studentHttpMapper.toRegisterCommand(googleUser));
        }

        // Generate JWT token
        String token = jwtTokenPort.generateToken(
                student.getId(),
                student.getName(),
                student.getEmail()
        );

        System.out.println("Token: " + token);

        // Build and return response
        try {
            AuthResponse response = AuthResponse.builder()
                    .access_token(token)
                    .type("Bearer")
                    .expiresIn(3600L)
                    .build();
            System.out.println("Authentication response: " + response);
            return response;
        } catch (Exception e) {
            System.out.println("Error building AuthResponse: " + e);
            throw e;
        }
    }
}