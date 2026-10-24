package com.uniflow.academic;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiTest extends IntegrationTestBase {

    @Test
    void protectedEndpointsRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/periods")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/courses")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/stats/courses")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }
}
