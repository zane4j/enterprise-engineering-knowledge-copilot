package io.github.zane4j.copilot.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.security.jwt")
public record JwtActorProperties(String tenantClaim, String userClaim) {

    public JwtActorProperties {
        tenantClaim = normalizeOrDefault(tenantClaim, "tenant_id");
        userClaim = normalizeOrDefault(userClaim, "user_id");
    }

    private static String normalizeOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
