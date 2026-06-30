package io.github.zane4j.copilot.ai;

import io.github.zane4j.copilot.rag.embedding.EmbeddingException;
import io.github.zane4j.copilot.rag.embedding.EmbeddingPort;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.embedding.EmbeddingModel;

final class SpringAiEmbeddingPort implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;
    private final String providerId;
    private final int dimensions;

    SpringAiEmbeddingPort(EmbeddingModel embeddingModel, String providerId, int dimensions) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        this.providerId = Objects.requireNonNull(providerId, "providerId must not be null");
        this.dimensions = dimensions;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            List<float[]> vectors = embeddingModel.embed(texts);
            if (vectors.size() != texts.size()) {
                throw new EmbeddingException("OpenAI returned an unexpected vector count");
            }
            for (float[] vector : vectors) {
                if (vector == null || vector.length != dimensions) {
                    throw new EmbeddingException(
                            "OpenAI embedding dimension does not match PostgreSQL vector(1536)");
                }
            }
            return List.copyOf(vectors);
        } catch (EmbeddingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EmbeddingException("Embedding provider request failed", exception);
        }
    }
}
