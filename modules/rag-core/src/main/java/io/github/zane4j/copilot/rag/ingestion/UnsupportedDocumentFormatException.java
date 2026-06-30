package io.github.zane4j.copilot.rag.ingestion;

public final class UnsupportedDocumentFormatException extends DocumentParseException {

    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }
}
