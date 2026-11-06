package com.uniflow.academic.config;

import com.uniflow.academic.shared.infrastructure.security.CognitoTokenValidators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

/**
 * Production decoder for Cognito access tokens. Tests (profile "test") provide their own JwtDecoder.
 */
@Configuration
@Profile("!test")
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.security.cognito.allowed-client-ids:}") String allowedClientIds
    ) {
        List<String> clientIds = CognitoTokenValidators.parseClientIds(allowedClientIds);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(CognitoTokenValidators.create(issuer, clientIds));
        return decoder;
    }
}
