package com.tooltrack.tooltrackbackend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDatabaseConfigTests {
    @Test
    void convertsRenderConnectionStringToJdbcCredentials() {
        var result = ProductionDatabaseConfig.parse(
                "postgresql://tooltrack_user:p%40ss%3Aword@dpg-example-a.oregon-postgres.render.com/tooltrack_db",
                "", "");

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-example-a.oregon-postgres.render.com:5432/tooltrack_db");
        assertThat(result.username()).isEqualTo("tooltrack_user");
        assertThat(result.password()).isEqualTo("p@ss:word");
    }

    @Test
    void acceptsExplicitJdbcConfiguration() {
        var result = ProductionDatabaseConfig.parse(
                "jdbc:postgresql://localhost:5432/tooltrack", "tooltrack", "secret");

        assertThat(result.username()).isEqualTo("tooltrack");
        assertThat(result.password()).isEqualTo("secret");
    }

    @Test
    void rejectsIncompleteJdbcConfiguration() {
        assertThatThrownBy(() -> ProductionDatabaseConfig.parse(
                "jdbc:postgresql://localhost:5432/tooltrack", "", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
