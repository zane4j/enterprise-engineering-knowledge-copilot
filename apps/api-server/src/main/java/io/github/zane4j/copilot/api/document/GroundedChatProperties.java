package io.github.zane4j.copilot.api.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.chat")
public record GroundedChatProperties(String provider, int maxEvidenceCharacters) {

    public GroundedChatProperties {
        if (maxEvidenceCharacters < 1_000 || maxEvidenceCharacters > 100_000) {
            throw new IllegalArgumentException("copilot.chat.max-evidence-characters must be between 1000 and 100000");
        }
    }
}
