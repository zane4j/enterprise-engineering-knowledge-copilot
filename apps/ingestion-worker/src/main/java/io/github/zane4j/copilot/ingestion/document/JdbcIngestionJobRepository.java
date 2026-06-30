package io.github.zane4j.copilot.ingestion.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zane4j.copilot.common.DomainException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcIngestionJobRepository implements IngestionJobRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcIngestionJobRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<DocumentToIngest> claimPendingJobs(int limit, int maxAttempts) {
        List<UUID> jobIds = jdbcTemplate.queryForList("""
                SELECT id
                FROM ingestion_jobs
                WHERE status = 'PENDING'
                  AND attempt_count < ?
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, UUID.class, maxAttempts, limit);
        if (jobIds.isEmpty()) {
            return List.of();
        }
        jdbcTemplate.batchUpdate("""
                UPDATE ingestion_jobs
                SET status = 'PROCESSING', attempt_count = attempt_count + 1, updated_at = now()
                WHERE id = ?
                """, jobIds, jobIds.size(), (statement, jobId) -> statement.setObject(1, jobId));
        return jdbcTemplate.query("""
                SELECT job.id AS ingestion_job_id,
                       job.tenant_id,
                       doc.knowledge_base_id,
                       doc.id AS document_id,
                       doc.version AS document_version,
                       doc.original_file_name,
                       doc.object_key,
                       doc.content_type,
                       job.attempt_count
                FROM ingestion_jobs job
                JOIN documents doc ON doc.id = job.document_id AND doc.tenant_id = job.tenant_id
                WHERE job.id = ANY (?)
                ORDER BY job.created_at
                """, this::mapDocument, jdbcTemplate.getDataSource());
    }

    @Override
    @Transactional
    public void saveChunksAndMarkSucceeded(DocumentToIngest document, List<PersistableChunk> chunks) {
        jdbcTemplate.update("""
                DELETE FROM document_chunks
                WHERE tenant_id = ?
                  AND document_id = ?
                  AND document_version = ?
                """, document.tenantId(), document.documentId(), document.documentVersion());

        jdbcTemplate.batchUpdate("""
                INSERT INTO document_chunks (
                    tenant_id, knowledge_base_id, document_id, document_version,
                    chunk_index, content, metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                """, chunks, chunks.size(), (statement, chunk) -> {
            statement.setObject(1, document.tenantId());
            statement.setObject(2, document.knowledgeBaseId());
            statement.setObject(3, document.documentId());
            statement.setInt(4, document.documentVersion());
            statement.setInt(5, chunk.index());
            statement.setString(6, chunk.content());
            statement.setString(7, toJson(chunk.metadata()));
        });

        jdbcTemplate.update("""
                UPDATE documents
                SET status = 'READY', failure_reason = NULL, updated_at = now()
                WHERE tenant_id = ? AND id = ?
                """, document.tenantId(), document.documentId());
        jdbcTemplate.update("""
                UPDATE ingestion_jobs
                SET status = 'SUCCEEDED', last_error = NULL, updated_at = now()
                WHERE tenant_id = ? AND id = ?
                """, document.tenantId(), document.ingestionJobId());
    }

    @Override
    @Transactional
    public void markFailed(DocumentToIngest document, String failureReason) {
        jdbcTemplate.update("""
                UPDATE documents
                SET status = 'FAILED', failure_reason = ?, updated_at = now()
                WHERE tenant_id = ? AND id = ?
                """, abbreviate(failureReason), document.tenantId(), document.documentId());
        jdbcTemplate.update("""
                UPDATE ingestion_jobs
                SET status = 'FAILED', last_error = ?, updated_at = now()
                WHERE tenant_id = ? AND id = ?
                """, abbreviate(failureReason), document.tenantId(), document.ingestionJobId());
    }

    private DocumentToIngest mapDocument(ResultSet resultSet, int rowNum) throws SQLException {
        return new DocumentToIngest(
                resultSet.getObject("ingestion_job_id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("knowledge_base_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getInt("document_version"),
                resultSet.getString("original_file_name"),
                resultSet.getString("object_key"),
                resultSet.getString("content_type"),
                resultSet.getInt("attempt_count"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException("CHUNK_METADATA_SERIALIZATION_FAILED", "Unable to serialize chunk metadata", exception);
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1_000 ? value : value.substring(0, 1_000);
    }
}
