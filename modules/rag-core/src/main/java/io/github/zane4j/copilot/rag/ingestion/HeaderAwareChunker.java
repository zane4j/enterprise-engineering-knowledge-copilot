package io.github.zane4j.copilot.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates bounded chunks while preserving the Markdown section heading in every chunk.
 * Overlap is applied to the body only so chunk provenance remains clear.
 */
public final class HeaderAwareChunker {

    private final int maxCharacters;
    private final int overlapCharacters;

    public HeaderAwareChunker(int maxCharacters, int overlapCharacters) {
        if (maxCharacters < 256) {
            throw new IllegalArgumentException("maxCharacters must be at least 256");
        }
        if (overlapCharacters < 0 || overlapCharacters >= maxCharacters) {
            throw new IllegalArgumentException("overlapCharacters must be non-negative and smaller than maxCharacters");
        }
        this.maxCharacters = maxCharacters;
        this.overlapCharacters = overlapCharacters;
    }

    public List<ChunkDraft> chunk(ParsedDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        List<ChunkDraft> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (ParsedDocument.Section section : document.sections()) {
            String heading = boundedHeading(section.heading());
            String prefix = "# " + heading + "\n\n";
            int bodyLimit = maxCharacters - prefix.length();
            List<String> units = splitIntoUnits(section.content(), bodyLimit);
            StringBuilder body = new StringBuilder();

            for (String unit : units) {
                if (body.length() == 0) {
                    body.append(unit);
                    continue;
                }
                int projectedLength = body.length() + 2 + unit.length();
                if (projectedLength <= bodyLimit) {
                    body.append("\n\n").append(unit);
                    continue;
                }

                chunks.add(toChunk(chunkIndex++, prefix, body, heading, section.startLine()));
                String overlap = trailingOverlap(body.toString());
                body.setLength(0);
                if (!overlap.isBlank() && overlap.length() + 2 + unit.length() <= bodyLimit) {
                    body.append(overlap).append("\n\n");
                }
                body.append(unit);
            }
            if (body.length() > 0) {
                chunks.add(toChunk(chunkIndex++, prefix, body, heading, section.startLine()));
            }
        }

        if (chunks.isEmpty()) {
            throw new DocumentParseException("Document does not contain any chunks after normalization");
        }
        return List.copyOf(chunks);
    }

    private String boundedHeading(String heading) {
        String normalized = heading == null || heading.isBlank() ? "Document" : heading.trim();
        int maxHeadingLength = Math.max(32, maxCharacters - 132);
        return normalized.length() <= maxHeadingLength
                ? normalized
                : normalized.substring(0, maxHeadingLength - 1).trim() + "…";
    }

    private ChunkDraft toChunk(int index, String prefix, StringBuilder body, String heading, int startLine) {
        String content = prefix + body.toString().trim();
        if (content.length() > maxCharacters) {
            throw new DocumentParseException("Chunk exceeds the configured maximum length");
        }
        return new ChunkDraft(index, content, heading, startLine);
    }

    private List<String> splitIntoUnits(String content, int bodyLimit) {
        List<String> units = new ArrayList<>();
        for (String paragraph : content.split("\\n\\s*\\n")) {
            String normalized = paragraph.trim();
            if (!normalized.isEmpty()) {
                units.addAll(splitLongUnit(normalized, bodyLimit));
            }
        }
        return units;
    }

    private List<String> splitLongUnit(String value, int limit) {
        if (value.length() <= limit) {
            return List.of(value);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int end = Math.min(value.length(), start + limit);
            if (end < value.length()) {
                int whitespace = findWhitespaceBefore(value, end, start + (limit / 2));
                if (whitespace > start) {
                    end = whitespace;
                }
            }
            String part = value.substring(start, end).trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
            start = end;
            while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
                start++;
            }
        }
        return parts;
    }

    private int findWhitespaceBefore(String value, int end, int lowerBound) {
        for (int index = end - 1; index >= lowerBound; index--) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private String trailingOverlap(String body) {
        if (overlapCharacters == 0 || body.length() <= overlapCharacters) {
            return overlapCharacters == 0 ? "" : body;
        }
        int start = body.length() - overlapCharacters;
        while (start < body.length() && !Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        return body.substring(Math.min(start, body.length())).trim();
    }
}
