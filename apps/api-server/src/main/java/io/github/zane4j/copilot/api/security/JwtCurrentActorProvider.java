package io.github.zane4j.copilot.api.security;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
class JwtCurrentActorProvider implements CurrentActorProvider {

    private final JwtActorProperties properties;
    private final UserIdentityRepository userIdentityRepository;

    JwtCurrentActorProvider(JwtActorProperties properties, UserIdentityRepository userIdentityRepository) {
        this.properties = properties;
        this.userIdentityRepository = userIdentityRepository;
    }

    @Override
    public CurrentActor requireCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            throw new ActorIdentityException("A verified JWT authentication is required");
        }

        Jwt jwt = jwtAuthentication.getToken();
        UUID tenantId = uuidClaim(jwt, properties.tenantClaim());
        UUID userId = uuidClaim(jwt, properties.userClaim());
        userIdentityRepository.requireTenantUser(tenantId, userId);
        return new CurrentActor(tenantId, userId);
    }

    private UUID uuidClaim(Jwt jwt, String claimName) {
        Object rawValue = jwt.getClaim(claimName);
        if (rawValue == null) {
            throw new ActorIdentityException("Required JWT actor claim is missing");
        }
        try {
            return UUID.fromString(String.valueOf(rawValue));
        } catch (IllegalArgumentException exception) {
            throw new ActorIdentityException("Required JWT actor claim is not a UUID");
        }
    }
}
