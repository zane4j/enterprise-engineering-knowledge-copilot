package io.github.zane4j.copilot.rag.ingestion;

import java.util.List;
import java.util.Objects;

/** Structured text extracted from one original document. */
public record ParsedDocument(String sourceFileName, List<Section> sections) {

    public ParsedDocument {
        Objects.requireNonNull(sourceFileName, "sourceFileName must not be null");
        sections = List.copyOf(Objects.requireNonNull(sections, "sections must not be null"));
        if (sections.isEmpty()) {
            throw new DocumentParseException("Document does not contain any indexable content");
        }
    }

    public record Section(String heading, int startLine, String content) {

        public Section {
            heading = Objects.requireNonNullElse(heading, "Document").trim();
            content = Objects.requireNonNullElse(content, "").trim();
            if (startLine < 1) {
                throw new IllegalArgumentException("startLine must be greater than zero");
            }
        }
    }
}
