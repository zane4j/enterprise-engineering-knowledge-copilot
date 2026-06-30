package io.github.zane4j.copilot.ingestion.document;

import java.util.List;

interface IngestionJobRepository {

    List<DocumentToIngest> claimPendingJobs(int limit, int maxAttempts);

    void saveChunksAndMarkSucceeded(DocumentToIngest document, List<PersistableChunk> chunks);

    void markFailed(DocumentToIngest document, String failureReason);
}
