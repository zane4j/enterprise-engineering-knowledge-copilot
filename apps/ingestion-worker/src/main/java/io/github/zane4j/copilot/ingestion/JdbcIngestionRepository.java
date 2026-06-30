package io.github.zane4j.copilot.ingestion;

import io.github.zane4j.copilot.rag.ingestion.ChunkDraft;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
class JdbcIngestionRepository {

    private static final int MAX_ERROR_LENGTH = 2_000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    JdbcIngestionRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    Optional<ClaimedIngestionJob> claimNext(int maxAttempts, long staleProcessingAfterSeconds) {
        ClaimedIngestionJob claimed = transactionTemplate.execute(status -> {
            List<ClaimedIngestionJob> candidates = jdbcTemplate.query("""
                    SELECT job.id AS job_id,
                           job.tenant_id,
                           job.document_id,
                           job.attempt_count,
                           doc.knowledge_base_id,
                           doc.object_key,
                           doc.original_file_name,
                           doc.content_type,
                           doc.version AS document_version
                    FROM ingestion_jobs job
                    JOIN documents doc ON doc.id = job.document_id
                    WHERE (
                        job.status = 'PENDING'
                        OR (job.status = 'PROCESSING'
                            AND job.updated_at < now() - (? * interval '1 second'))
                    )
                      AND job.attempt_count < ?
                      AND doc.status IN ('PENDING', 'PROCESSING')
                    ORDER BY job.created_at
                    LIMIT 1
                    FOR UPDATE OF job, doc SKIP LOCKED
                    """, this::mapClaimedJob, staleProcessingAfterSeconds, maxAttempts);

            if (candidates.isEmpty()) {
                return null;
            }

            ClaimedIngestionJob candidate = candidates.getFirst();
            int claimedAttempt = candidate.attemptCount() + 1;
            jdbcTemplate.update("""
                    UPDATE ingestion_jobs
                    SET status = 'PROCESSING', attempt_count = ?, last_error = NULL, updated_at = now()
                    WHERE id = ?
                    """, claimedAttempt, candidate.jobId());
            jdbcTemplate.update("""
                    UPDATE documents
                    SET status = 'PROCESSING', failure_reason = NULL, updated_at = now()
                    WHERE id = ? AND tenant_id = ?
                    """, candidate.documentId(), candidate.tenantId());
            return candidate.withAttemptCount(claimedAttempt);
        });
        return Optional.ofNullable(claimed);
    }

    void complete(ClaimedIngestionJob job, List<ChunkDraft> chunks) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("""
                    DELETE FROM document_chunks
                    WHERE tenant_id = ? AND document_id = ? AND document_version = ?
                    """, job.tenantId(), job.documentId(), job.documentVersion());

            jdbcTemplate.batchUpdate("""
                    INSERT INTO document_chunks (
                        id, tenant_id, knowledge_base_id, document_id, document_version,
                        chunk_index, content, metadata
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    """, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int index) throws SQLException {
                    ChunkDraft chunk = chunks.get(index);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, job.tenantId());
                    statement.setObject(3, job.knowledgeBaseId());
                    statement.setObject(4, job.documentId());
                    statement.setInt(5, job.documentVersion());
                    statement.setInt(6, chunk.index());
                    statement.setString(7, chunk.content());
                    statement.setString(8, metadataJson(job.originalFileName(), job.contentType(), chunk));
                }

                @Override
                public int getBatchSize() {
                    return chunks.size();
                }
            });

            jdbcTemplate.update("""
                    UPDATE documents
                    SET status = 'READY', failure_reason = NULL, updated_at = now()
                    WHERE id = ? AND tenant_id = ?
                    """, job.documentId(), job.tenantId());
            jdbcTemplate.update("""
                    UPDATE ingestion_jobs
                    SET status = 'SUCCEEDED', last_error = NULL, updated_at = now()
                    WHERE id = ? AND tenant_id = ?
                    """, job.jobId(), job.tenantId());
        });
    }

    void recordFailure(ClaimedIngestionJob job, Throwable failure, boolean retryable, int maxAttempts) {
        boolean terminal = !retryable || job.attemptCount() >= maxAttempts;
        String message = safeFailureMessage(failure);
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("""
                    UPDATE ingestion_jobs
                    SET status = ?, last_error = ?, updated_at = now()
                    WHERE id = ? AND tenant_id = ?
                    """, terminal ? "FAILED" : "PENDING", message, job.jobId(), job.tenantId());
            jdbcTemplate.update("""
                    UPDATE documents
                    SET status = ?, failure_reason = ?, updated_at = now()
                    WHERE id = ? AND tenant_id = ?
                    """, terminal ? "FAILED" : "PENDING", message, job.documentId(), job.tenantId());
        });
    }

    private ClaimedIngestionJob mapClaimedJob(ResultSet resultSet, int rowNum) throws SQLException {
        return new ClaimedIngestionJob(
                resultSet.getObject("job_id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getObject("knowledge_base_id", UUID.class),
                resultSet.getString("object_key"),
                resultSet.getString("original_file_name"),
                resultSet.getString("content_type"),
                resultSet.getInt("document_version"),
                resultSet.getInt("attempt_count"));
    }

    private String metadataJson(String sourceFileName, String contentType, ChunkDraft chunk) {
        return ("{\"sourceFileName\":\"%s\",\"contentType\":\"%s\",\"sectionTitle\":\"%s\","
                + "\"sectionStartLine\":%d,\"chunkIndex\":%d,\"characterCount\":%d}")
                .formatted(
                        escapeJson(sourceFileName),
                        escapeJson(contentType),
                        escapeJson(chunk.sectionTitle()),
                        chunk.sectionStartLine(),
                        chunk.index(),
                        chunk.content().length());
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String safeFailureMessage(Throwable failure) {
        String message = failure.getClass().getSimpleName() + ": "
                + (failure.getMessage() == null ? "No detail" : failure.getMessage());
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    record ClaimedIngestionJob(
            UUID jobId,
            UUID tenantId,
            UUID documentId,
            UUID knowledgeBaseId,
            String objectKey,
            String originalFileName,
            String contentType,
            int documentVersion,
            int attemptCount) {

        ClaimedIngestionJob withAttemptCount(int nextAttemptCount) {
            return new ClaimedIngestionJob(
                    jobId, tenantId, documentId, knowledgeBaseId, objectKey, originalFileName,
                    contentType, documentVersion, nextAttemptCount);
        }
    }
}
