package io.github.zane4j.copilot.rag.embedding;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic local-development embedding provider. It is useful for wiring and tests, but is
 * not a substitute for a production semantic embedding model.
 */
public final class HashEmbeddingPort implements EmbeddingPort {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}_-]+");

    private final int dimensions;

    public HashEmbeddingPort(int dimensions) {
        if (dimensions < 1) {
            throw new IllegalArgumentException("dimensions must be greater than zero");
        }
        this.dimensions = dimensions;
    }

    @Override
    public String providerId() {
        return "hash-dev-v1";
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Objects.requireNonNull(texts, "texts must not be null");
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embedOne(text));
        }
        return List.copyOf(vectors);
    }

    private float[] embedOne(String input) {
        String normalized = Normalizer.normalize(
                Objects.requireNonNull(input, "text must not be null"), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        float[] vector = new float[dimensions];
        int tokenCount = 0;
        while (matcher.find()) {
            String token = matcher.group();
            int hash = mix(token.hashCode());
            int index = Math.floorMod(hash, dimensions);
            vector[index] += (hash & 1) == 0 ? 1.0f : -1.0f;
            tokenCount++;
        }
        if (tokenCount == 0) {
            throw new EmbeddingException("Cannot embed text without indexable tokens");
        }

        double squaredLength = 0.0;
        for (float value : vector) {
            squaredLength += value * value;
        }
        float inverseLength = (float) (1.0 / Math.sqrt(squaredLength));
        for (int index = 0; index < vector.length; index++) {
            vector[index] *= inverseLength;
        }
        return vector;
    }

    private int mix(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        mixed ^= mixed >>> 16;
        return mixed;
    }
}
