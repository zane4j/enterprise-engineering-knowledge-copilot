package io.github.zane4j.copilot.api.document;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

interface IngestionJobRepository {

    void create(UUID jobId, UUID tenantId, UUID documentId);

    Optional<IngestionJob> findById(UUID tenantId, UUID jobId);

    record IngestionJob(
            UUID id,
            UUID documentId,
            String status,
            int attemptCount,
            String lastError,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
