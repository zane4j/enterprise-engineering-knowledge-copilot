package io.github.zane4j.copilot.api.security;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-only actor provider backed by the deterministic seed data in Docker Compose.
 * It will be replaced by a JWT-backed implementation before the multi-tenant API is exposed.
 */
@Component
@Profile("local")
class DemoCurrentActorProvider implements CurrentActorProvider {

    private static final CurrentActor DEMO_ACTOR = new CurrentActor(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"));

    @Override
    public CurrentActor requireCurrentActor() {
        return DEMO_ACTOR;
    }
}
