package com.uniflow.academic;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicServiceApplicationTests extends IntegrationTestBase {

    @Test
    void contextLoadsAndFlywayAppliesBaseline() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic.flyway_schema_history WHERE version = '1' AND success", Integer.class);
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'academic'",
                String.class);

        assertThat(applied).isEqualTo(1);
        assertThat(tables).contains("periods", "courses");
    }
}
