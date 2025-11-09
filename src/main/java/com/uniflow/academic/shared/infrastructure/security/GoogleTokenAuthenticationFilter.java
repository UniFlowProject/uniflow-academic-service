package com.uniflow.academic.shared.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtro de autenticación que valida tokens de Google OAuth2
 * Usa el ID único de Google (sub) como identificador principal
 */
@Slf4j
@Component
public class GoogleTokenAuthenticationFilter extends OncePerRequestFilter {

    private final GoogleTokenValidator tokenValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleTokenAuthenticationFilter(GoogleTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Extrae token del header
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || authHeader.isBlank()) {
                log.debug("No Authorization header found");
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Valida formato "Bearer <token>"
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Extrae el token
            String accessToken = authHeader.replace("Bearer ", "");

            // 4. Valida con Google
            Map<String, Object> tokenInfo = tokenValidator.validateToken(accessToken);

            // 5. Si token no es válido, retorna 401
            if (tokenInfo == null) {
                handleUnauthorized(response, "Invalid or expired Google token");
                return;
            }

            // 6. Extrae Google ID único (sub)
            String googleId = (String) tokenInfo.get("id");
            String email = (String) tokenInfo.get("email");

            if (googleId == null || googleId.isBlank()) {
                log.error("Google token missing 'sub' claim");
                handleUnauthorized(response, "Invalid Google token structure");
                return;
            }

            // 7. Token es válido - crea Authentication con Google ID
            OAuth2User oauth2User = createOAuth2User(tokenInfo, googleId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            oauth2User,
                            null,
                            oauth2User.getAuthorities()
                    );

            // 8. Guarda en SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User authenticated - Google ID: {}, Email: {}", googleId, email);

        } catch (Exception e) {
            log.error("Token validation filter error", e);
            handleUnauthorized(response, "Token validation failed");
            return;
        }

        // Continúa con el siguiente filtro
        filterChain.doFilter(request, response);
    }

    /**
     * Crea un OAuth2User desde la respuesta de Google
     * Usa el Google ID (sub) como nombre principal
     */
    private OAuth2User createOAuth2User(
            Map<String, Object> tokenInfo,
            String googleId
    ) {
        Map<String, Object> attributes = new HashMap<>(tokenInfo);

        // El segundo parámetro es el name attribute key
        // Usamos "sub" en lugar de "email" para el identificador único
        return new DefaultOAuth2User(
                List.of(),  // Sin authorities por ahora
                attributes,
                "id"       // Usar Google ID como atributo principal
        );
    }

    /**
     * Maneja respuesta 401
     */
    private void handleUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", message);

        response.getWriter()
                .write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    /**
     * Solo procesa endpoints que requieren token
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/health")
                || path.equals("/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars");
    }
}