package io.github.zane4j.copilot.ingestion.document;

import java.util.Map;

record PersistableChunk(int index, String content, Map<String, String> metadata) {
}
