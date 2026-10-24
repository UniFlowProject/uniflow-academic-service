package com.uniflow.academic.period;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeriodStatisticsApiTest extends IntegrationTestBase {

    @Test
    void statisticsCoverAllPeriodsNotJustTheFirstPage() throws Exception {
        String user = newUserId();
        for (int i = 0; i < 12; i++) {
            createPeriod(user, "P" + i);
        }

        mockMvc.perform(getAs(user, "/periods/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.finished").value(12));
    }
}
