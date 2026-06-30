package io.github.zane4j.copilot.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.embedding")
public record EmbeddingProperties(String provider, String model, int dimensions, int batchSize) {

    public static final int PGVECTOR_DIMENSIONS = 1536;

    public EmbeddingProperties {
        provider = provider == null || provider.isBlank() ? "hash" : provider.trim().toLowerCase();
        model = model == null || model.isBlank() ? "text-embedding-3-small" : model.trim();
        if (!provider.equals("hash") && !provider.equals("openai")) {
            throw new IllegalArgumentException("copilot.embedding.provider must be hash or openai");
        }
        if (dimensions == 0) {
            dimensions = PGVECTOR_DIMENSIONS;
        }
        if (dimensions != PGVECTOR_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "copilot.embedding.dimensions must match document_chunks.embedding vector(1536)");
        }
        if (batchSize == 0) {
            batchSize = 32;
        }
        if (batchSize < 1 || batchSize > 256) {
            throw new IllegalArgumentException("copilot.embedding.batch-size must be between 1 and 256");
        }
    }
}
