package io.github.zane4j.copilot.rag.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HeaderAwareTextChunkerTests {

    @Test
    void keepsMarkdownHeaderAsChunkMetadata() {
        HeaderAwareTextChunker chunker = new HeaderAwareTextChunker(400, 40);

        List<DocumentChunk> chunks = chunker.chunk("# Payment Runbook\n\nCheck database pool metrics.", "MARKDOWN");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().metadata()).containsEntry("sectionTitle", "Payment Runbook");
        assertThat(chunks.getFirst().metadata()).containsEntry("documentType", "MARKDOWN");
    }
}
