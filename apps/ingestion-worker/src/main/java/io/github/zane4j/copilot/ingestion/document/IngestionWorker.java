package io.github.zane4j.copilot.ingestion.document;

import io.github.zane4j.copilot.rag.chunking.DocumentChunk;
import io.github.zane4j.copilot.rag.chunking.HeaderAwareTextChunker;
import io.github.zane4j.copilot.storage.ObjectStoragePort;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "copilot.ingestion", name = "enabled", havingValue = "true", matchIfMissing = true)
class IngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(IngestionWorker.class);

    private final IngestionJobRepository ingestionJobRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentParser documentParser;
    private final HeaderAwareTextChunker chunker;
    private final int maxAttempts;

    IngestionWorker(
            IngestionJobRepository ingestionJobRepository,
            ObjectStoragePort objectStoragePort,
            DocumentParser documentParser,
            @Value("${copilot.ingestion.max-attempts:3}") int maxAttempts) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.objectStoragePort = objectStoragePort;
        this.documentParser = documentParser;
        this.chunker = new HeaderAwareTextChunker();
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${copilot.ingestion.polling-delay-ms:5000}")
    void poll() {
        List<DocumentToIngest> documents = ingestionJobRepository.claimPendingJobs(5, maxAttempts);
        for (DocumentToIngest document : documents) {
            process(document);
        }
    }

    private void process(DocumentToIngest document) {
        try (InputStream inputStream = objectStoragePort.get(document.objectKey())) {
            ParsedDocument parsed = documentParser.parse(document, inputStream);
            List<PersistableChunk> chunks = chunker.chunk(parsed.text(), parsed.documentType()).stream()
                    .map(this::toPersistableChunk)
                    .toList();
            if (chunks.isEmpty()) {
                ingestionJobRepository.markFailed(document, "Document contains no ingestible text");
                return;
            }
            ingestionJobRepository.saveChunksAndMarkSucceeded(document, chunks);
            log.info("Ingested document {} with {} chunks", document.documentId(), chunks.size());
        } catch (Exception exception) {
            ingestionJobRepository.markFailed(document, exception.getMessage());
            log.warn("Failed to ingest document {}", document.documentId(), exception);
        }
    }

    private PersistableChunk toPersistableChunk(DocumentChunk chunk) {
        return new PersistableChunk(chunk.index(), chunk.content(), chunk.metadata());
    }
}
