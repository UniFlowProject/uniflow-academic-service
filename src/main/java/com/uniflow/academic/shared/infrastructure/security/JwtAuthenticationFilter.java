package com.uniflow.academic.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class GoogleTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // ✅ RUTAS QUE NO REQUIEREN AUTENTICACIÓN
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/auth/google/callback",
            "/auth/refresh",
            "/health",
            "/students/",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/"
    );

    public GoogleTokenAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("Processing request: {}", requestPath);

        // ✅ Extraer token del header
        String token = extractTokenFromHeader(request);

        if (token != null && isValidToken(token)) {
            try {
                String userId = jwtProvider.getUserIdFromToken(token);

                // Crear autenticación
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null, new ArrayList<>()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("✅ JWT validated for user: {}", userId);

            } catch (Exception e) {
                log.warn("⚠️ Failed to validate JWT token", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    // ✅ SKIP filter para rutas no protegidas
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {

        String requestPath = request.getRequestURI();

        boolean shouldSkip = EXCLUDED_PATHS.stream()
                .anyMatch(requestPath::contains);

        if (shouldSkip) {
            log.debug("Skipping authentication filter for: {}", requestPath);
        }

        return shouldSkip;
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            log.debug("No Authorization header found");
            return null;
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format");
            return null;
        }

        return authHeader.substring("Bearer ".length());
    }

    private boolean isValidToken(String token) {
        try {
            return jwtProvider.validateToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}