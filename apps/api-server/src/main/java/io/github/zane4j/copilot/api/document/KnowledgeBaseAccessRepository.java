package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.common.DomainException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class KnowledgeBaseAccessRepository {

    private static final Set<String> WRITE_ROLES = Set.of("OWNER", "ADMIN", "EDITOR");

    private final JdbcTemplate jdbcTemplate;

    KnowledgeBaseAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void requireReadAccess(UUID tenantId, UUID userId, UUID knowledgeBaseId) {
        roleFor(tenantId, userId, knowledgeBaseId);
    }

    void requireWriteAccess(UUID tenantId, UUID userId, UUID knowledgeBaseId) {
        String role = roleFor(tenantId, userId, knowledgeBaseId);
        if (!WRITE_ROLES.contains(role)) {
            throw new DomainException("KNOWLEDGE_BASE_WRITE_FORBIDDEN", "Write access to this knowledge base is required");
        }
    }

    private String roleFor(UUID tenantId, UUID userId, UUID knowledgeBaseId) {
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT member.role
                FROM knowledge_bases kb
                JOIN knowledge_base_members member ON member.knowledge_base_id = kb.id
                WHERE kb.tenant_id = ?
                  AND kb.id = ?
                  AND member.user_id = ?
                """, String.class, tenantId, knowledgeBaseId, userId);
        if (roles.isEmpty()) {
            throw new DomainException("KNOWLEDGE_BASE_ACCESS_DENIED", "Access to this knowledge base is denied");
        }
        return roles.getFirst();
    }
}
