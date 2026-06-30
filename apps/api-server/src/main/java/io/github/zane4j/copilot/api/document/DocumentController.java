package io.github.zane4j.copilot.api.document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/documents")
class DocumentController {

    private final DocumentUploadService documentUploadService;

    DocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentUploadResponse> upload(
            @PathVariable UUID knowledgeBaseId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.accepted().body(toResponse(documentUploadService.upload(knowledgeBaseId, file)));
    }

    @PostMapping("/{documentId}/reindex")
    ResponseEntity<DocumentUploadResponse> reindex(
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId) {
        return ResponseEntity.accepted().body(toResponse(documentUploadService.reindex(knowledgeBaseId, documentId)));
    }

    @GetMapping
    List<DocumentSummaryResponse> list(@PathVariable UUID knowledgeBaseId) {
        return documentUploadService.listDocuments(knowledgeBaseId).stream()
                .map(document -> new DocumentSummaryResponse(
                        document.id(),
                        document.originalFileName(),
                        document.contentType(),
                        document.status(),
                        document.version(),
                        document.createdAt(),
                        document.updatedAt()))
                .toList();
    }

    private DocumentUploadResponse toResponse(DocumentUploadService.UploadedDocument uploaded) {
        return new DocumentUploadResponse(uploaded.documentId(), uploaded.ingestionJobId(), uploaded.status());
    }

    record DocumentUploadResponse(UUID documentId, UUID ingestionJobId, String status) {
    }

    record DocumentSummaryResponse(
            UUID id,
            String originalFileName,
            String contentType,
            String status,
            int version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
