package io.github.zane4j.copilot.ai;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.chat")
public record ChatProperties(String provider) {

    public ChatProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider.trim().toLowerCase(Locale.ROOT);
        if (!provider.equals("none") && !provider.equals("openai")) {
            throw new IllegalArgumentException("copilot.chat.provider must be none or openai");
        }
    }
}
