package com.showdown.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "showdown.security")
public record SecurityProperties(
        String adminUser,
        String adminPassword,
        String refereeUser,
        String refereePassword,
        String playerUser,
        String playerPassword
) {
}
