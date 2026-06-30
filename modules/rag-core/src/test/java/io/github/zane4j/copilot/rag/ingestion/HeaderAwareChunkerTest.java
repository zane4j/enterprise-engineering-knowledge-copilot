package io.github.zane4j.copilot.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HeaderAwareChunkerTest {

    @Test
    void includesTheSectionHeadingInEveryChunk() {
        String paragraph = "A connection pool can become exhausted when slow queries hold connections for too long. ";
        ParsedDocument document = new ParsedDocument(
                "payment-runbook.md",
                List.of(new ParsedDocument.Section("Database Connection Pool", 3, paragraph.repeat(12))));
        HeaderAwareChunker chunker = new HeaderAwareChunker(256, 32);

        List<ChunkDraft> chunks = chunker.chunk(document);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().startsWith("# Database Connection Pool")));
        assertFalse(chunks.stream().anyMatch(chunk -> chunk.content().length() > 256));
    }
}
