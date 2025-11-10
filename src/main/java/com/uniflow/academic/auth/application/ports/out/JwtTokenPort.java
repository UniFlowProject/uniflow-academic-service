package com.uniflow.academic.auth.application.ports.out;

public interface JwtTokenPort {
    String generateToken(String subject, String name, String email);
    boolean validateToken(String token);
    String getSubjectFromToken(String token);
}