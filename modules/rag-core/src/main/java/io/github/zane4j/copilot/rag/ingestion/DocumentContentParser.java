package io.github.zane4j.copilot.rag.ingestion;

import java.io.InputStream;

public interface DocumentContentParser {

    ParsedDocument parse(String sourceFileName, InputStream input, int maxContentCharacters);
}
