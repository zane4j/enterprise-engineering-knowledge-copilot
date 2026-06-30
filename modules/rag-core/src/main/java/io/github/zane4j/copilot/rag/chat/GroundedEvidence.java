package io.github.zane4j.copilot.rag.chat;

import io.github.zane4j.copilot.rag.Citation;
import java.util.Objects;

/** A retrieved source frozen before model invocation and exposed as an application citation. */
public record GroundedEvidence(String sourceId, String content, Citation citation) {

    public GroundedEvidence {
        sourceId = requireNonBlank(sourceId, "sourceId");
        content = requireNonBlank(content, "content");
        citation = Objects.requireNonNull(citation, "citation must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
