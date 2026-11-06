package com.uniflow.academic.shared.infrastructure.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validation rules for Cognito access tokens: issuer and timestamps, {@code token_use=access}
 * and a {@code client_id} from an allow-list.
 */
public final class CognitoTokenValidators {

    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String CLIENT_ID_CLAIM = "client_id";
    private static final String ACCESS_TOKEN_USE = "access";

    private CognitoTokenValidators() {
    }

    public static OAuth2TokenValidator<Jwt> create(String issuer, List<String> allowedClientIds) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                accessTokenFromAllowedClient(Set.copyOf(allowedClientIds))
        );
    }

    public static List<String> parseClientIds(String commaSeparated) {
        List<String> ids = commaSeparated == null
                ? List.of()
                : Arrays.stream(commaSeparated.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .toList();
        if (ids.isEmpty()) {
            throw new IllegalStateException("COGNITO_ALLOWED_CLIENT_IDS must contain at least one client id");
        }
        return ids;
    }

    private static OAuth2TokenValidator<Jwt> accessTokenFromAllowedClient(Set<String> allowedClientIds) {
        return jwt -> {
            if (!ACCESS_TOKEN_USE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))) {
                return failure("The token is not an access token");
            }
            String clientId = jwt.getClaimAsString(CLIENT_ID_CLAIM);
            if (clientId == null || !allowedClientIds.contains(clientId)) {
                return failure("The token was not issued to an allowed client");
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
