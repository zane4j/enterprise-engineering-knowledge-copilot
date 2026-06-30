package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.api.security.CurrentActor;
import io.github.zane4j.copilot.api.security.CurrentActorProvider;
import io.github.zane4j.copilot.common.DomainException;
import io.github.zane4j.copilot.storage.ObjectStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
class DocumentUploadService {

    private final CurrentActorProvider currentActorProvider;
    private final KnowledgeBaseAccessRepository knowledgeBaseAccessRepository;
    private final DocumentRepository documentRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final ObjectStoragePort objectStoragePort;
    private final TransactionTemplate transactionTemplate;
    private final DocumentUploadValidator validator;

    DocumentUploadService(
            CurrentActorProvider currentActorProvider,
            KnowledgeBaseAccessRepository knowledgeBaseAccessRepository,
            DocumentRepository documentRepository,
            IngestionJobRepository ingestionJobRepository,
            ObjectStoragePort objectStoragePort,
            TransactionTemplate transactionTemplate,
            @Value("${copilot.upload.max-file-size-bytes}") long maxFileSizeBytes) {
        this.currentActorProvider = currentActorProvider;
        this.knowledgeBaseAccessRepository = knowledgeBaseAccessRepository;
        this.documentRepository = documentRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.objectStoragePort = objectStoragePort;
        this.transactionTemplate = transactionTemplate;
        this.validator = new DocumentUploadValidator(maxFileSizeBytes);
    }

    UploadedDocument upload(UUID knowledgeBaseId, MultipartFile file) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        knowledgeBaseAccessRepository.requireWriteAccess(actor.tenantId(), actor.userId(), knowledgeBaseId);
        DocumentUploadValidator.ValidatedUpload upload = validator.validate(file);

        UUID documentId = UUID.randomUUID();
        UUID ingestionJobId = UUID.randomUUID();
        String objectKey = objectKey(actor.tenantId(), knowledgeBaseId, documentId, upload.fileName());
        ObjectStoragePort.StoredObject storedObject = putObject(file, upload, objectKey);

        try {
            UploadedDocument result = transactionTemplate.execute(status -> {
                documentRepository.create(new DocumentRepository.CreateDocumentCommand(
                        documentId,
                        actor.tenantId(),
                        knowledgeBaseId,
                        upload.fileName(),
                        storedObject.objectKey(),
                        upload.contentType(),
                        storedObject.checksum(),
                        actor.userId()));
                ingestionJobRepository.create(ingestionJobId, actor.tenantId(), documentId);
                return new UploadedDocument(documentId, ingestionJobId, "PENDING");
            });
            return Objects.requireNonNull(result, "Document upload transaction returned no result");
        } catch (RuntimeException exception) {
            deleteAfterPersistenceFailure(storedObject.objectKey(), exception);
            throw exception;
        }
    }

    UploadedDocument reindex(UUID knowledgeBaseId, UUID documentId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        knowledgeBaseAccessRepository.requireWriteAccess(actor.tenantId(), actor.userId(), knowledgeBaseId);
        UUID ingestionJobId = UUID.randomUUID();

        UploadedDocument result = transactionTemplate.execute(status -> {
            boolean scheduled = documentRepository.scheduleReingestion(actor.tenantId(), knowledgeBaseId, documentId);
            if (!scheduled) {
                throw new DomainException("DOCUMENT_NOT_FOUND", "Document was not found in this knowledge base");
            }
            ingestionJobRepository.create(ingestionJobId, actor.tenantId(), documentId);
            return new UploadedDocument(documentId, ingestionJobId, "PENDING");
        });
        return Objects.requireNonNull(result, "Document reindex transaction returned no result");
    }

    List<DocumentRepository.DocumentSummary> listDocuments(UUID knowledgeBaseId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        knowledgeBaseAccessRepository.requireReadAccess(actor.tenantId(), actor.userId(), knowledgeBaseId);
        return documentRepository.findAllByKnowledgeBase(actor.tenantId(), knowledgeBaseId);
    }

    IngestionJobRepository.IngestionJob getJob(UUID ingestionJobId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        return ingestionJobRepository.findById(actor.tenantId(), ingestionJobId)
                .orElseThrow(() -> new DomainException("INGESTION_JOB_NOT_FOUND", "Ingestion job was not found"));
    }

    private ObjectStoragePort.StoredObject putObject(
            MultipartFile file,
            DocumentUploadValidator.ValidatedUpload upload,
            String objectKey) {
        try (InputStream input = file.getInputStream()) {
            return objectStoragePort.put(
                    new ObjectStoragePort.UploadCommand(objectKey, upload.contentType(), upload.contentLength()),
                    input);
        } catch (IOException exception) {
            throw new DomainException("FILE_READ_FAILED", "Unable to read uploaded document", exception);
        }
    }

    private void deleteAfterPersistenceFailure(String objectKey, RuntimeException originalException) {
        try {
            objectStoragePort.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private String objectKey(UUID tenantId, UUID knowledgeBaseId, UUID documentId, String fileName) {
        String safeFileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return "tenant/%s/knowledge-base/%s/document/%s/original/%s"
                .formatted(tenantId, knowledgeBaseId, documentId, safeFileName);
    }

    record UploadedDocument(UUID documentId, UUID ingestionJobId, String status) {
    }
}
