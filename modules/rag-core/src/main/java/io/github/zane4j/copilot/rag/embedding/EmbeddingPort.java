package io.github.zane4j.copilot.rag.embedding;

import java.util.List;
import java.util.Objects;

/** Provider-neutral contract for generating embedding vectors. */
public interface EmbeddingPort {

    String providerId();

    int dimensions();

    List<float[]> embed(List<String> texts);

    default float[] embed(String text) {
        Objects.requireNonNull(text, "text must not be null");
        List<float[]> vectors = embed(List.of(text));
        if (vectors.size() != 1) {
            throw new EmbeddingException("Embedding provider returned an unexpected vector count");
        }
        return vectors.getFirst();
    }
}
