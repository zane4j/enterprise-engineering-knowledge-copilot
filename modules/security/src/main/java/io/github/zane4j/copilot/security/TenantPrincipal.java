package io.github.zane4j.copilot.security;

import io.github.zane4j.copilot.domain.TenantId;
import java.util.Objects;
import java.util.UUID;

public record TenantPrincipal(TenantId tenantId, UUID userId) {

    public TenantPrincipal {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
