package com.uniflow.academic.auth.application.ports.out;

public interface JwtTokenPort {
    String generateToken(String subject, String name, String email, String picture);
    boolean validateToken(String token);
    String getSubjectFromToken(String token);
}