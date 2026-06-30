package io.github.zane4j.copilot.api.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIngestionJobRepository implements IngestionJobRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcIngestionJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID jobId, UUID tenantId, UUID documentId) {
        jdbcTemplate.update("""
                INSERT INTO ingestion_jobs (id, tenant_id, document_id, status)
                VALUES (?, ?, ?, 'PENDING')
                """, jobId, tenantId, documentId);
    }

    @Override
    public Optional<IngestionJob> findById(UUID tenantId, UUID jobId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, status, attempt_count, last_error, created_at, updated_at
                FROM ingestion_jobs
                WHERE tenant_id = ? AND id = ?
                """, this::mapJob, tenantId, jobId).stream().findFirst();
    }

    private IngestionJob mapJob(ResultSet resultSet, int rowNum) throws SQLException {
        return new IngestionJob(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("status"),
                resultSet.getInt("attempt_count"),
                resultSet.getString("last_error"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
