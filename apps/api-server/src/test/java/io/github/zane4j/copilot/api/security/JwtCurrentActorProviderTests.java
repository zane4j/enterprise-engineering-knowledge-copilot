package io.github.zane4j.copilot.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentActorProviderTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAndChecksTenantBoundActorClaims() {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        AtomicReference<CurrentActor> checkedActor = new AtomicReference<>();
        UserIdentityRepository identityRepository = (tenant, user) -> checkedActor.set(new CurrentActor(tenant, user));
        JwtCurrentActorProvider provider = new JwtCurrentActorProvider(
                new JwtActorProperties("tenant_id", "user_id"), identityRepository);

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(tenantId.toString(), userId.toString())));

        assertThat(provider.requireCurrentActor()).isEqualTo(new CurrentActor(tenantId, userId));
        assertThat(checkedActor.get()).isEqualTo(new CurrentActor(tenantId, userId));
    }

    @Test
    void rejectsMalformedActorClaimBeforeDatabaseLookup() {
        UserIdentityRepository identityRepository = (tenant, user) -> {
            throw new AssertionError("Database lookup must not run for malformed claims");
        };
        JwtCurrentActorProvider provider = new JwtCurrentActorProvider(
                new JwtActorProperties("tenant_id", "user_id"), identityRepository);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt("not-a-uuid", "00000000-0000-0000-0000-000000000002")));

        assertThatThrownBy(provider::requireCurrentActor)
                .isInstanceOf(ActorIdentityException.class);
    }

    private Jwt jwt(String tenantId, String userId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("tenant_id", tenantId)
                .claim("user_id", userId)
                .build();
    }
}
