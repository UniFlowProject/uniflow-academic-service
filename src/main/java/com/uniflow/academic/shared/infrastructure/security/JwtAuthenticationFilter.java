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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // RUTAS QUE NO REQUIEREN AUTENTICACIÓN
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/auth/google/callback",
            "/auth/refresh",
            "/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/"
    );

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String token = extractTokenFromHeader(request);

        log.debug("Extracted token: {}", token);

        if (token != null) {
            try {
                if (jwtProvider.validateToken(token)) {
                    String userId = jwtProvider.getUserIdFromToken(token);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userId,
                            null,
                            new ArrayList<>()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("✅ JWT validated for user: {}", userId);
                } else {
                    log.warn("❌ Invalid JWT token");
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to validate JWT token: {}", e.getMessage());
            }
        } else {
            log.debug("No token found in request: {}", requestPath);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        log.debug("Authorization header received: {}", authHeader);

        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format");
            return null;
        }

        return authHeader.substring("Bearer ".length());
    }
}