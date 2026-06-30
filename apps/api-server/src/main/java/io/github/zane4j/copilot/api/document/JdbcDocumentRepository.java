package io.github.zane4j.copilot.api.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDocumentRepository implements DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(CreateDocumentCommand command) {
        jdbcTemplate.update("""
                INSERT INTO documents (
                    id, tenant_id, knowledge_base_id, original_file_name, object_key,
                    content_type, checksum, status, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
                """,
                command.documentId(),
                command.tenantId(),
                command.knowledgeBaseId(),
                command.originalFileName(),
                command.objectKey(),
                command.contentType(),
                command.checksum(),
                command.createdBy());
    }

    @Override
    public List<DocumentSummary> findAllByKnowledgeBase(UUID tenantId, UUID knowledgeBaseId) {
        return jdbcTemplate.query("""
                SELECT id, original_file_name, content_type, status, version, created_at, updated_at
                FROM documents
                WHERE tenant_id = ?
                  AND knowledge_base_id = ?
                  AND status <> 'DELETED'
                ORDER BY created_at DESC
                """, this::mapSummary, tenantId, knowledgeBaseId);
    }

    @Override
    public boolean scheduleReingestion(UUID tenantId, UUID knowledgeBaseId, UUID documentId) {
        return jdbcTemplate.update("""
                UPDATE documents
                SET status = 'PENDING',
                    version = version + 1,
                    failure_reason = NULL,
                    updated_at = now()
                WHERE id = ?
                  AND tenant_id = ?
                  AND knowledge_base_id = ?
                  AND status IN ('READY', 'FAILED')
                """, documentId, tenantId, knowledgeBaseId) == 1;
    }

    private DocumentSummary mapSummary(ResultSet resultSet, int rowNum) throws SQLException {
        return new DocumentSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("original_file_name"),
                resultSet.getString("content_type"),
                resultSet.getString("status"),
                resultSet.getInt("version"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
