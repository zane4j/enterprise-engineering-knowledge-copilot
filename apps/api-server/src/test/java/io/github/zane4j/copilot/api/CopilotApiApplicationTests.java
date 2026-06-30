package io.github.zane4j.copilot.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zane4j.copilot.api.document.DocumentUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CopilotApiApplicationTests {

    @Test
    void acceptsMarkdownDocumentWithinTheConfiguredLimit() {
        DocumentUploadValidator validator = new DocumentUploadValidator(1024);
        MockMultipartFile file = new MockMultipartFile(
                "file", "payment-runbook.md", "text/markdown", "# Runbook".getBytes());

        DocumentUploadValidator.ValidatedUpload validated = validator.validate(file);

        assertThat(validated.fileName()).isEqualTo("payment-runbook.md");
        assertThat(validated.contentType()).isEqualTo("text/markdown");
    }
}
