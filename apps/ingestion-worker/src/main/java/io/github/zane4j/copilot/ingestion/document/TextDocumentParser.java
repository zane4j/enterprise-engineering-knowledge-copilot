package io.github.zane4j.copilot.ingestion.document;

import io.github.zane4j.copilot.common.DomainException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class TextDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parse(DocumentToIngest document, InputStream inputStream) {
        String contentType = document.contentType().toLowerCase(Locale.ROOT);
        String fileName = document.originalFileName().toLowerCase(Locale.ROOT);
        if (contentType.equals("application/pdf") || fileName.endsWith(".pdf")) {
            throw new DomainException("PDF_PARSING_NOT_IMPLEMENTED", "PDF parsing will be implemented in the next ingestion increment");
        }
        if (!isSupportedTextFile(contentType, fileName)) {
            throw new DomainException("DOCUMENT_TYPE_UNSUPPORTED", "Only Markdown and plain text ingestion is implemented in this increment");
        }
        try {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String type = fileName.endsWith(".md") || fileName.endsWith(".markdown") ? "MARKDOWN" : "TEXT";
            return new ParsedDocument(text, type);
        } catch (IOException exception) {
            throw new DomainException("DOCUMENT_READ_FAILED", "Unable to read original document object", exception);
        }
    }

    private boolean isSupportedTextFile(String contentType, String fileName) {
        return contentType.startsWith("text/")
                || fileName.endsWith(".txt")
                || fileName.endsWith(".md")
                || fileName.endsWith(".markdown");
    }
}
