package io.github.zane4j.copilot.ingestion;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.zane4j.copilot.ingestion.config.IngestionProperties;
import org.junit.jupiter.api.Test;

class IngestionPropertiesTest {

    @Test
    void rejectsAnOverlapThatIsNotSmallerThanChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> new IngestionProperties(
                true, 3, 1_000, 300, 1_200, 1_200, 10_000));
    }
}
