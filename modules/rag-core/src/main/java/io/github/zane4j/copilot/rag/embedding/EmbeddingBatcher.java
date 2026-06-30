package io.github.zane4j.copilot.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EmbeddingBatcher {

    private EmbeddingBatcher() {
    }

    public static List<float[]> embedAll(EmbeddingPort embeddingPort, List<String> texts, int batchSize) {
        Objects.requireNonNull(embeddingPort, "embeddingPort must not be null");
        Objects.requireNonNull(texts, "texts must not be null");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (texts.isEmpty()) {
            return List.of();
        }

        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(texts.size(), start + batchSize);
            List<float[]> batch = embeddingPort.embed(texts.subList(start, end));
            if (batch.size() != end - start) {
                throw new EmbeddingException("Embedding provider returned an unexpected vector count");
            }
            validateDimensions(embeddingPort, batch);
            vectors.addAll(batch);
        }
        return List.copyOf(vectors);
    }

    private static void validateDimensions(EmbeddingPort embeddingPort, List<float[]> vectors) {
        for (float[] vector : vectors) {
            if (vector == null || vector.length != embeddingPort.dimensions()) {
                throw new EmbeddingException("Embedding provider returned a vector with an unexpected dimension");
            }
        }
    }
}
