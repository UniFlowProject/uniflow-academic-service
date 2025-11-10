package com.uniflow.academic.auth.infrastructure.adapters;

import com.uniflow.academic.auth.application.ports.out.JwtTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:3600000}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(String subject, String name, String email) {
        System.out.println("Generating token...");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name);
        claims.put("email", email);

        System.out.println("Claims: " + claims);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .signWith(getSigningKey())  // ← USAR HS256
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
//                    .(getSigningKey())
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
//                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSubjectFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
//                .getBody();
                .getPayload();

        return claims.getSubject();
    }
}