package io.github.zane4j.copilot.ingestion;

import io.github.zane4j.copilot.ingestion.JdbcIngestionRepository.ClaimedIngestionJob;
import io.github.zane4j.copilot.ingestion.config.IngestionProperties;
import io.github.zane4j.copilot.rag.ingestion.DocumentContentParser;
import io.github.zane4j.copilot.rag.ingestion.DocumentParseException;
import io.github.zane4j.copilot.rag.ingestion.HeaderAwareChunker;
import io.github.zane4j.copilot.rag.ingestion.ParsedDocument;
import io.github.zane4j.copilot.storage.ObjectStoragePort;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class IngestionPollingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionPollingWorker.class);

    private final JdbcIngestionRepository ingestionRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentContentParser documentContentParser;
    private final HeaderAwareChunker headerAwareChunker;
    private final IngestionProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    IngestionPollingWorker(
            JdbcIngestionRepository ingestionRepository,
            ObjectStoragePort objectStoragePort,
            DocumentContentParser documentContentParser,
            HeaderAwareChunker headerAwareChunker,
            IngestionProperties properties) {
        this.ingestionRepository = ingestionRepository;
        this.objectStoragePort = objectStoragePort;
        this.documentContentParser = documentContentParser;
        this.headerAwareChunker = headerAwareChunker;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${copilot.ingestion.poll-delay-ms:5000}")
    void poll() {
        if (!properties.enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            ingestionRepository.claimNext(properties.maxAttempts(), properties.staleProcessingAfterSeconds())
                    .ifPresent(this::process);
        } finally {
            running.set(false);
        }
    }

    private void process(ClaimedIngestionJob job) {
        try (InputStream input = objectStoragePort.get(job.objectKey())) {
            ParsedDocument document = documentContentParser.parse(
                    job.originalFileName(), input, properties.maxContentCharacters());
            var chunks = headerAwareChunker.chunk(document);
            ingestionRepository.complete(job, chunks);
            LOGGER.info("Ingestion completed: jobId={}, documentId={}, chunks={}",
                    job.jobId(), job.documentId(), chunks.size());
        } catch (DocumentParseException exception) {
            ingestionRepository.recordFailure(job, exception, false, properties.maxAttempts());
            LOGGER.warn("Ingestion rejected: jobId={}, documentId={}, reason={}",
                    job.jobId(), job.documentId(), exception.getMessage());
        } catch (Exception exception) {
            ingestionRepository.recordFailure(job, exception, true, properties.maxAttempts());
            LOGGER.error("Ingestion failed: jobId={}, documentId={}", job.jobId(), job.documentId(), exception);
        }
    }
}
