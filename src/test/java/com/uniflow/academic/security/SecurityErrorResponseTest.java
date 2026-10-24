package com.uniflow.academic.security;

import com.uniflow.academic.support.IntegrationTestBase;
import com.uniflow.academic.shared.infrastructure.web.JsonAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityErrorResponseTest extends IntegrationTestBase {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JsonAccessDeniedHandler accessDeniedHandler;

    @Test
    void missingTokenReturnsJsonUnauthorizedBody() throws Exception {
        mockMvc.perform(get("/periods"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }

    @Test
    void invalidOrExpiredTokenReturnsJsonUnauthorizedBodyWithoutEchoingTheToken() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Jwt expired at 2020-01-01"));
        try {
            String body = mockMvc.perform(get("/periods").header("Authorization", "Bearer secret-token-value"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("secret-token-value").doesNotContain("expired").doesNotContain("Exception");
        } finally {
            reset(jwtDecoder);
        }
    }

    @Test
    void accessDeniedReturnsJsonForbiddenBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("secret detail"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"FORBIDDEN\"")
                .doesNotContain("secret detail");
    }
}
