package io.github.zane4j.copilot.ingestion.document;

import java.util.UUID;

record DocumentToIngest(
        UUID ingestionJobId,
        UUID tenantId,
        UUID knowledgeBaseId,
        UUID documentId,
        int documentVersion,
        String originalFileName,
        String objectKey,
        String contentType,
        int attemptCount) {
}
