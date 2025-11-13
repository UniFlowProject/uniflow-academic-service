package com.uniflow.academic.auth.infrastructure.adapters;

import com.uniflow.academic.auth.application.ports.out.GoogleAuthPort;
import com.uniflow.academic.student.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleAuthAdapter implements GoogleAuthPort {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Override
    public Student validateAuthorizationCode(String code, String redirectUri) {
        System.out.println(this.getClass().getSimpleName() + "validateAuthorizationCode");
        // Exchange code for token
        String accessToken = exchangeCodeForToken(code, redirectUri);

        // Get user info with token
        Map<String, Object> userInfo = getUserInfo(accessToken);

        // Map to student domain object
        return Student.builder()
                .id((String) userInfo.get("sub"))
                .name((String) userInfo.get("name"))
                .email((String) userInfo.get("email"))
//                .provider("google")
//                .providerId((String) userInfo.get("sub"))
//                .studentId((String) userInfo.get("email"))
                .avatar((String) userInfo.get("picture"))
                .accessToken(accessToken)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        System.out.println(this.getClass().getSimpleName() +  " | exangeCodeForToken | " + code + " | " + redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        System.out.println("body: " + body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(tokenUri, request, Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("Failed to exchange authorization code for token");
        }

        return (String) response.get("access_token");
    }

    private Map<String, Object> getUserInfo(String accessToken) {
        System.out.println(this.getClass().getSimpleName() +  " | getUserInfo | " + accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.getForObject(userInfoUri + "?access_token=" + accessToken, Map.class);

        if (response == null || !response.containsKey("sub")) {
            throw new IllegalStateException("Failed to get user info from Google");
        }

        return response;
    }
}