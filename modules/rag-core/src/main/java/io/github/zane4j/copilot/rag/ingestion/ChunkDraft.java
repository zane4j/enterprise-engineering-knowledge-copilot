package io.github.zane4j.copilot.rag.ingestion;

import java.util.Objects;

/** A metadata-rich chunk ready for persistence and later embedding. */
public record ChunkDraft(
        int index,
        String content,
        String sectionTitle,
        int sectionStartLine) {

    public ChunkDraft {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        content = Objects.requireNonNull(content, "content must not be null").trim();
        sectionTitle = Objects.requireNonNullElse(sectionTitle, "Document").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content must not be empty");
        }
        if (sectionStartLine < 1) {
            throw new IllegalArgumentException("sectionStartLine must be greater than zero");
        }
    }
}
