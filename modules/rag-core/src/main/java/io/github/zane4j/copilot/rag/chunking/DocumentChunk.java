package io.github.zane4j.copilot.rag.chunking;

import java.util.Map;

public record DocumentChunk(int index, String content, Map<String, String> metadata) {

    public DocumentChunk {
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
