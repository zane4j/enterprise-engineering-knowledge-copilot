package io.github.zane4j.copilot.ingestion.document;

import java.io.InputStream;

interface DocumentParser {

    ParsedDocument parse(DocumentToIngest document, InputStream inputStream);
}
