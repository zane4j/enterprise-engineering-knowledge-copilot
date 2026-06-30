package io.github.zane4j.copilot.api.document;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion-jobs")
class IngestionJobController {

    private final DocumentUploadService documentUploadService;

    IngestionJobController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @GetMapping("/{ingestionJobId}")
    IngestionJobResponse get(@PathVariable UUID ingestionJobId) {
        IngestionJobRepository.IngestionJob job = documentUploadService.getJob(ingestionJobId);
        return new IngestionJobResponse(
                job.id(), job.documentId(), job.status(), job.attemptCount(),
                job.lastError(), job.createdAt(), job.updatedAt());
    }

    record IngestionJobResponse(
            UUID id,
            UUID documentId,
            String status,
            int attemptCount,
            String lastError,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
