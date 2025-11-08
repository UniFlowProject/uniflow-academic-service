package com.uniflow.academic.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.Map;

/**
 * Valida tokens de Google OAuth2
 * Similar a: https://oauth2.googleapis.com/tokeninfo?access_token={token}
 */
@Slf4j
@Component
public class GoogleTokenValidator {

    private static final String GOOGLE_TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Valida un token de Google
     *
     * @param accessToken Token JWT desde Authorization header
     * @return Map con información del token si es válido, null si no
     */
    public Map<String, Object> validateToken(String accessToken) {
        try {
            log.debug("Validating token with Google: {}",
                    accessToken.substring(0, 20) + "...");

            // Construir URL con token
            String url = GOOGLE_TOKENINFO_URL + "?access_token=" + accessToken;

            // Llamar a Google
            Map<String, Object> response = restTemplate.getForObject(
                    url,
                    Map.class
            );

            assert response != null;
            log.debug("Token validated successfully. User: {}",
                    response.get("email"));

            return response;

        } catch (RestClientException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return null;
        }
    }
}