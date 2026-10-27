package com.healthcare.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.driver-class-name:}")
    private String configuredDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        // 1. Check Render / Heroku / Cloud environment variable DATABASE_URL
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("DB_URL");
        }

        HikariConfig config = new HikariConfig();

        if (envDbUrl != null && !envDbUrl.isBlank()) {
            try {
                // If Render format: postgres://user:password@host:port/database or postgresql://...
                if (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://")) {
                    log.info("Detected Render/Cloud PostgreSQL DATABASE_URL format. Converting to JDBC...");
                    URI uri = new URI(envDbUrl.replace("postgresql://", "postgres://"));
                    String userInfo = uri.getUserInfo();
                    String username = null;
                    String password = null;

                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    }

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String dbName = (path != null && path.length() > 1) ? path.substring(1) : "";

                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + dbName;
                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        jdbcUrl += "?" + uri.getQuery();
                    }

                    config.setJdbcUrl(jdbcUrl);
                    if (username != null) config.setUsername(username);
                    if (password != null) config.setPassword(password);
                    config.setDriverClassName("org.postgresql.Driver");
                    log.info("Configured PostgreSQL JDBC URL: jdbc:postgresql://{}:{}/{}", uri.getHost(), port, dbName);
                } else {
                    // Standard JDBC URL
                    config.setJdbcUrl(envDbUrl);
                    if (System.getenv("DB_USERNAME") != null) config.setUsername(System.getenv("DB_USERNAME"));
                    else if (configuredUsername != null) config.setUsername(configuredUsername);

                    if (System.getenv("DB_PASSWORD") != null) config.setPassword(System.getenv("DB_PASSWORD"));
                    else if (configuredPassword != null) config.setPassword(configuredPassword);
                }
            } catch (Exception e) {
                log.warn("Failed to parse cloud DATABASE_URL, falling back to properties: {}", e.getMessage());
                fallbackToProperties(config);
            }
        } else {
            fallbackToProperties(config);
        }

        // Pool tuning for Render free & starter tiers
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);

        return new HikariDataSource(config);
    }

    private void fallbackToProperties(HikariConfig config) {
        config.setJdbcUrl(configuredUrl);
        config.setUsername(configuredUsername);
        config.setPassword(configuredPassword);
        if (configuredDriver != null && !configuredDriver.isBlank()) {
            config.setDriverClassName(configuredDriver);
        }
    }
}
