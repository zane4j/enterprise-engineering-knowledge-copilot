package io.github.zane4j.copilot.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.ingestion")
public record IngestionProperties(
        boolean enabled,
        int maxAttempts,
        long pollDelayMs,
        long staleProcessingAfterSeconds,
        int maxChunkCharacters,
        int chunkOverlapCharacters,
        int maxContentCharacters) {

    public IngestionProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (pollDelayMs < 100) {
            throw new IllegalArgumentException("pollDelayMs must be at least 100");
        }
        if (staleProcessingAfterSeconds < 1) {
            throw new IllegalArgumentException("staleProcessingAfterSeconds must be greater than zero");
        }
        if (maxChunkCharacters < 256) {
            throw new IllegalArgumentException("maxChunkCharacters must be at least 256");
        }
        if (chunkOverlapCharacters < 0 || chunkOverlapCharacters >= maxChunkCharacters) {
            throw new IllegalArgumentException("chunkOverlapCharacters must be non-negative and smaller than maxChunkCharacters");
        }
        if (maxContentCharacters < maxChunkCharacters) {
            throw new IllegalArgumentException("maxContentCharacters must be greater than maxChunkCharacters");
        }
    }
}
