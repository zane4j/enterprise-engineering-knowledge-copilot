package io.github.zane4j.copilot.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PgVectorLiteralTest {

    @Test
    void formatsAValidPgVectorLiteral() {
        assertEquals("[1.0,-2.5,0.0]", PgVectorLiteral.of(new float[] {1.0f, -2.5f, 0.0f}));
    }
}
