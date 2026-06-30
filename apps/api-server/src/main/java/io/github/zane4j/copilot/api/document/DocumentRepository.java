package io.github.zane4j.copilot.api.document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface DocumentRepository {

    void create(CreateDocumentCommand command);

    List<DocumentSummary> findAllByKnowledgeBase(UUID tenantId, UUID knowledgeBaseId);

    boolean scheduleReingestion(UUID tenantId, UUID knowledgeBaseId, UUID documentId);

    record CreateDocumentCommand(
            UUID documentId,
            UUID tenantId,
            UUID knowledgeBaseId,
            String originalFileName,
            String objectKey,
            String contentType,
            String checksum,
            UUID createdBy) {
    }

    record DocumentSummary(
            UUID id,
            String originalFileName,
            String contentType,
            String status,
            int version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
