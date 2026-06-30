package io.github.zane4j.copilot.rag.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses UTF-8 TXT and Markdown documents. Every Markdown heading starts a logical section.
 * PDF intentionally remains unsupported until a dedicated parser is added.
 */
public final class PlainTextAndMarkdownParser implements DocumentContentParser {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");

    @Override
    public ParsedDocument parse(String sourceFileName, InputStream input, int maxContentCharacters) {
        String extension = extensionOf(sourceFileName);
        if (!extension.equals("txt") && !extension.equals("md") && !extension.equals("markdown")) {
            throw new UnsupportedDocumentFormatException(
                    "The ingestion worker currently supports TXT and Markdown only: " + sourceFileName);
        }
        String text = readUtf8(input, maxContentCharacters);
        if (text.isBlank()) {
            throw new DocumentParseException("Document does not contain readable text");
        }
        return extension.equals("txt")
                ? new ParsedDocument(sourceFileName, List.of(new ParsedDocument.Section("Document", 1, text)))
                : parseMarkdown(sourceFileName, text);
    }

    private ParsedDocument parseMarkdown(String sourceFileName, String text) {
        List<ParsedDocument.Section> sections = new ArrayList<>();
        String[] lines = text.split("\\n", -1);
        String heading = "Document";
        int sectionStartLine = 1;
        StringBuilder body = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Matcher matcher = MARKDOWN_HEADING.matcher(line);
            if (matcher.matches()) {
                addSectionIfContentExists(sections, heading, sectionStartLine, body);
                heading = matcher.group(2).trim();
                sectionStartLine = index + 1;
                body.setLength(0);
                continue;
            }
            body.append(line).append('\n');
        }
        addSectionIfContentExists(sections, heading, sectionStartLine, body);
        if (sections.isEmpty()) {
            throw new DocumentParseException("Markdown document does not contain indexable text");
        }
        return new ParsedDocument(sourceFileName, sections);
    }

    private void addSectionIfContentExists(
            List<ParsedDocument.Section> sections,
            String heading,
            int sectionStartLine,
            StringBuilder body) {
        String content = body.toString().trim();
        if (!content.isEmpty()) {
            sections.add(new ParsedDocument.Section(heading, sectionStartLine, content));
        }
    }

    private String readUtf8(InputStream input, int maxContentCharacters) {
        if (maxContentCharacters < 1) {
            throw new IllegalArgumentException("maxContentCharacters must be greater than zero");
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (text.length() + read > maxContentCharacters) {
                    throw new DocumentParseException("Document text exceeds the configured parsing limit");
                }
                text.append(buffer, 0, read);
            }
        } catch (IOException exception) {
            throw new DocumentParseException("Unable to read document text", exception);
        }
        return text.toString().replace("\u0000", "").replace("\r\n", "\n").replace('\r', '\n');
    }

    private String extensionOf(String sourceFileName) {
        int index = sourceFileName.lastIndexOf('.');
        if (index < 1 || index == sourceFileName.length() - 1) {
            throw new UnsupportedDocumentFormatException("Document file extension is required");
        }
        return sourceFileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
