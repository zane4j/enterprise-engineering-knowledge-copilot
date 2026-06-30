package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.common.DomainException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public final class DocumentUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "md", "markdown", "txt");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "application/octet-stream");

    private final long maxFileSizeBytes;

    public DocumentUploadValidator(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("FILE_EMPTY", "A non-empty document file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new DomainException("FILE_TOO_LARGE", "Document file exceeds the configured upload limit");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new DomainException("FILE_TYPE_UNSUPPORTED", "Only PDF, Markdown, and text files are supported");
        }

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new DomainException("FILE_MEDIA_TYPE_UNSUPPORTED", "The file media type is not supported");
        }
        return new ValidatedUpload(fileName, contentType, file.getSize());
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new DomainException("FILE_NAME_MISSING", "Document file name is required");
        }
        String normalized = originalFileName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new DomainException("FILE_NAME_INVALID", "Document file name is invalid");
        }
        return normalized;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index == fileName.length() - 1) {
            throw new DomainException("FILE_EXTENSION_MISSING", "Document file extension is required");
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public record ValidatedUpload(String fileName, String contentType, long contentLength) {
    }
}
