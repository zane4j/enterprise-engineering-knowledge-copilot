package io.github.zane4j.copilot.api.security;

import java.util.UUID;

public record CurrentActor(UUID tenantId, UUID userId) {
}
