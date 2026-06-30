package io.github.zane4j.copilot.rag.chunking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight chunker for Markdown and plain text.
 *
 * <p>It keeps Markdown headings as metadata when available and falls back to a fixed-size window for long sections.
 * This is intentionally dependency-light; PDF parsing and model-specific tokenization are handled in later increments.
 */
public final class HeaderAwareTextChunker {

    private static final Pattern MARKDOWN_HEADER = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final int DEFAULT_MAX_CHARS = 1_200;
    private static final int DEFAULT_OVERLAP_CHARS = 160;

    private final int maxChars;
    private final int overlapChars;

    public HeaderAwareTextChunker() {
        this(DEFAULT_MAX_CHARS, DEFAULT_OVERLAP_CHARS);
    }

    public HeaderAwareTextChunker(int maxChars, int overlapChars) {
        if (maxChars < 200) {
            throw new IllegalArgumentException("maxChars must be at least 200");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars must be non-negative and smaller than maxChars");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<DocumentChunk> chunk(String rawText, String documentType) {
        String normalized = normalize(rawText);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<Section> sections = splitIntoSections(normalized);
        List<DocumentChunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            for (String part : splitSection(section.body())) {
                if (!part.isBlank()) {
                    Map<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("documentType", documentType == null ? "TEXT" : documentType);
                    if (section.heading() != null && !section.heading().isBlank()) {
                        metadata.put("sectionTitle", section.heading());
                    }
                    chunks.add(new DocumentChunk(chunks.size(), part.strip(), metadata));
                }
            }
        }
        return chunks;
    }

    private String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private List<Section> splitIntoSections(String text) {
        List<Section> sections = new ArrayList<>();
        String currentHeading = null;
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\n")) {
            Matcher matcher = MARKDOWN_HEADER.matcher(line);
            if (matcher.matches()) {
                addSection(sections, currentHeading, current);
                currentHeading = matcher.group(2).strip();
                current.append(line).append('\n');
            } else {
                current.append(line).append('\n');
            }
        }
        addSection(sections, currentHeading, current);
        return sections.isEmpty() ? List.of(new Section(null, text)) : sections;
    }

    private void addSection(List<Section> sections, String heading, StringBuilder body) {
        String content = body.toString().strip();
        if (!content.isBlank()) {
            sections.add(new Section(heading, content));
        }
        body.setLength(0);
    }

    private List<String> splitSection(String section) {
        if (section.length() <= maxChars) {
            return List.of(section);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < section.length()) {
            int end = Math.min(section.length(), start + maxChars);
            int boundary = findBoundary(section, start, end);
            parts.add(section.substring(start, boundary));
            if (boundary == section.length()) {
                break;
            }
            start = Math.max(boundary - overlapChars, start + 1);
        }
        return parts;
    }

    private int findBoundary(String text, int start, int hardEnd) {
        if (hardEnd == text.length()) {
            return hardEnd;
        }
        int paragraph = text.lastIndexOf("\n\n", hardEnd);
        if (paragraph > start + 200) {
            return paragraph;
        }
        int sentence = Math.max(text.lastIndexOf(". ", hardEnd), text.lastIndexOf("。", hardEnd));
        if (sentence > start + 200) {
            return Math.min(sentence + 1, text.length());
        }
        int whitespace = text.lastIndexOf(' ', hardEnd);
        if (whitespace > start + 200) {
            return whitespace;
        }
        return hardEnd;
    }

    private record Section(String heading, String body) {
    }
}
