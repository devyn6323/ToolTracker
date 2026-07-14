package com.tooltrack.tooltrackbackend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("prod")
public class ProductionDatabaseConfig {
    @Bean
    DataSource dataSource(Environment environment) {
        DatabaseCredentials credentials = parse(
                environment.getRequiredProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_USERNAME", ""),
                environment.getProperty("DATABASE_PASSWORD", ""));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.jdbcUrl());
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());
        config.setMaximumPoolSize(environment.getProperty("DB_MAX_POOL_SIZE", Integer.class, 10));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setPoolName("ToolTrackPool");
        return new HikariDataSource(config);
    }

    static DatabaseCredentials parse(String databaseUrl, String configuredUsername, String configuredPassword) {
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            if (configuredUsername.isBlank() || configuredPassword.isBlank()) {
                throw new IllegalArgumentException("DATABASE_USERNAME and DATABASE_PASSWORD are required with a JDBC DATABASE_URL");
            }
            return new DatabaseCredentials(databaseUrl, configuredUsername, configuredPassword);
        }
        if (!databaseUrl.startsWith("postgresql://") && !databaseUrl.startsWith("postgres://")) {
            throw new IllegalArgumentException("DATABASE_URL must use postgresql://, postgres://, or jdbc:postgresql://");
        }

        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("DATABASE_URL must include a username and password");
        }
        String[] parts = userInfo.split(":", 2);
        String username = decode(parts[0]);
        String password = decode(parts[1]);
        int port = uri.getPort() < 0 ? 5432 : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query;
        return new DatabaseCredentials(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record DatabaseCredentials(String jdbcUrl, String username, String password) {
    }
}
