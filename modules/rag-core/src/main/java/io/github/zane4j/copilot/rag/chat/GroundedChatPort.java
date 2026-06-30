package io.github.zane4j.copilot.rag.chat;

import java.util.Objects;
import reactor.core.publisher.Flux;

/** Provider-neutral, streaming text generation over an already grounded prompt. */
public interface GroundedChatPort {

    Flux<String> stream(GroundedChatRequest request);

    record GroundedChatRequest(String systemPrompt, String userPrompt) {

        public GroundedChatRequest {
            systemPrompt = requireNonBlank(systemPrompt, "systemPrompt");
            userPrompt = requireNonBlank(userPrompt, "userPrompt");
        }

        private static String requireNonBlank(String value, String fieldName) {
            String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalized;
        }
    }
}
