package io.github.zane4j.copilot.rag;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A source that supports a generated answer. */
public record Citation(
        UUID documentId,
        String documentName,
        String locator,
        double score,
        Map<String, String> metadata) {

    public Citation {
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(documentName, "documentName must not be null");
        Objects.requireNonNull(locator, "locator must not be null");
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
