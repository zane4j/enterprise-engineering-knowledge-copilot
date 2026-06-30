package io.github.zane4j.copilot.rag.embedding;

import java.util.Objects;

/** Formats finite float vectors for PostgreSQL's vector literal cast. */
public final class PgVectorLiteral {

    private PgVectorLiteral() {
    }

    public static String of(float[] vector) {
        Objects.requireNonNull(vector, "vector must not be null");
        if (vector.length == 0) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        StringBuilder literal = new StringBuilder(vector.length * 10 + 2).append('[');
        for (int index = 0; index < vector.length; index++) {
            float value = vector[index];
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("vector values must be finite");
            }
            if (index > 0) {
                literal.append(',');
            }
            literal.append(Float.toString(value));
        }
        return literal.append(']').toString();
    }
}
