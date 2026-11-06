package com.uniflow.academic.security;

import com.uniflow.academic.config.JwtDecoderConfig;
import com.uniflow.academic.shared.infrastructure.security.CognitoTokenValidators;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitoTokenValidatorsTest {

    private static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_pool";
    private static final String CLIENT = "client-a";

    private final OAuth2TokenValidator<Jwt> validator =
            CognitoTokenValidators.create(ISSUER, List.of(CLIENT, "client-b"));

    private Jwt jwt(Consumer<Jwt.Builder> customizer) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuer(ISSUER)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("token_use", "access")
                .claim("client_id", CLIENT);
        customizer.accept(builder);
        return builder.build();
    }

    private OAuth2TokenValidatorResult validate(Consumer<Jwt.Builder> customizer) {
        return validator.validate(jwt(customizer));
    }

    @Test
    void accessTokenFromAllowedClientIsAccepted() {
        assertThat(validate(b -> { }).hasErrors()).isFalse();
        assertThat(validate(b -> b.claim("client_id", "client-b")).hasErrors()).isFalse();
    }

    @Test
    void idTokenIsRejected() {
        assertThat(validate(b -> b.claim("token_use", "id")).hasErrors()).isTrue();
    }

    @Test
    void unknownClientIsRejected() {
        assertThat(validate(b -> b.claim("client_id", "other-client")).hasErrors()).isTrue();
    }

    @Test
    void wrongIssuerIsRejected() {
        assertThat(validate(b -> b.issuer("https://evil.example.com/pool")).hasErrors()).isTrue();
    }

    @Test
    void missingTokenUseIsRejected() {
        assertThat(validate(b -> b.claims(c -> c.remove("token_use"))).hasErrors()).isTrue();
    }

    @Test
    void missingClientIdIsRejected() {
        assertThat(validate(b -> b.claims(c -> c.remove("client_id"))).hasErrors()).isTrue();
    }

    @Test
    void expiredTokenIsRejected() {
        OAuth2TokenValidatorResult result = validate(b -> b
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void blankAllowedClientIdsFailFast() {
        assertThatThrownBy(() -> CognitoTokenValidators.parseClientIds(" , "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COGNITO_ALLOWED_CLIENT_IDS");
        assertThatThrownBy(() -> CognitoTokenValidators.parseClientIds(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionDecoderFailsFastWithoutAllowedClientIds() {
        assertThatThrownBy(() -> new JwtDecoderConfig().jwtDecoder(ISSUER, ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clientIdsAreSplitAndTrimmed() {
        assertThat(CognitoTokenValidators.parseClientIds(" a, b ,,c")).containsExactly("a", "b", "c");
    }
}
