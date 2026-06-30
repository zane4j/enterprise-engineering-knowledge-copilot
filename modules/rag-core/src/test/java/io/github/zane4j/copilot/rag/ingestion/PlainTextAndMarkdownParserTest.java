package io.github.zane4j.copilot.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PlainTextAndMarkdownParserTest {

    private final PlainTextAndMarkdownParser parser = new PlainTextAndMarkdownParser();

    @Test
    void splitsMarkdownAtHeadingsAndPreservesSectionStartLines() {
        String markdown = "# Connection Pool\nCheck active connections.\n\n## Slow SQL\nReview long-running queries.";

        ParsedDocument document = parser.parse(
                "payment-runbook.md",
                new ByteArrayInputStream(markdown.getBytes(StandardCharsets.UTF_8)),
                10_000);

        assertEquals(2, document.sections().size());
        assertEquals("Connection Pool", document.sections().getFirst().heading());
        assertEquals(1, document.sections().getFirst().startLine());
        assertEquals("Slow SQL", document.sections().get(1).heading());
        assertEquals(4, document.sections().get(1).startLine());
    }
}
