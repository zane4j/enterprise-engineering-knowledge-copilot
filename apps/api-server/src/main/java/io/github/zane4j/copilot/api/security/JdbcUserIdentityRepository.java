package io.github.zane4j.copilot.api.security;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcUserIdentityRepository implements UserIdentityRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcUserIdentityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void requireTenantUser(UUID tenantId, UUID userId) {
        Long matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users
                WHERE tenant_id = ?
                  AND id = ?
                """, Long.class, tenantId, userId);
        if (matches == null || matches == 0L) {
            throw new ActorIdentityException("The authenticated token is not mapped to a user in this tenant");
        }
    }
}
