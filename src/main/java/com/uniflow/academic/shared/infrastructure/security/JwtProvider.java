package com.uniflow.academic.shared.infrastructure.security;

import com.uniflow.academic.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // ✅ Crear clave simétrica desde el secret
        this.key = Keys.hmacShaKeyFor(
                jwtProperties.getSecretKey()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generar JWT token con claims
     */
    public String generateToken(String studentId, Map<String, Object> claims) {
        return createToken(claims, studentId);
    }

    /**
     * Validar JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            log.debug("✅ Token validated successfully");
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("⚠️ JWT token has expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("⚠️ JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("⚠️ Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("⚠️ Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extraer userId (sub) del token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (JwtException e) {
            log.error("❌ Error extracting userId from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    /**
     * Extraer cualquier claim del token
     */
    public Object getClaimFromToken(String token, String claimName) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get(claimName);
        } catch (JwtException e) {
            log.error("❌ Error extracting claim from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extraer email del token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return (String) claims.get("email");
        } catch (JwtException e) {
            log.error("❌ Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extraer nombre del token
     */
    public String getNameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return (String) claims.get("name");
        } catch (JwtException e) {
            log.error("❌ Error extracting name from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verificar si el token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            log.warn("⚠️ Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Crear JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

            return Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuer(jwtProperties.getIssuer())
                    .audience()
                    .add(jwtProperties.getAudience())
                    .and()
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

        } catch (Exception e) {
            log.error("❌ Error creating JWT token: {}", e.getMessage());
            throw new RuntimeException("Could not create JWT token", e);
        }
    }
}