package io.github.zane4j.copilot.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashEmbeddingPortTest {

    @Test
    void producesDeterministicNormalizedVectors() {
        HashEmbeddingPort embeddingPort = new HashEmbeddingPort(1536);

        float[] first = embeddingPort.embed("database connection pool exhaustion");
        float[] second = embeddingPort.embed("database connection pool exhaustion");

        assertEquals(1536, first.length);
        assertArrayEquals(first, second);
        double squaredLength = 0.0;
        for (float value : first) {
            squaredLength += value * value;
        }
        assertEquals(1.0, squaredLength, 0.0001);
    }
}
