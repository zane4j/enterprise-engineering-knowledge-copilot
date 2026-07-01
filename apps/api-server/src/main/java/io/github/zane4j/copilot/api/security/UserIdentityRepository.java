package io.github.zane4j.copilot.api.security;

import java.util.UUID;

interface UserIdentityRepository {

    void requireTenantUser(UUID tenantId, UUID userId);
}
