package io.github.zane4j.copilot.rag;

import java.util.List;
import java.util.Objects;

/**
 * Model-agnostic port for generating a source-grounded answer.
 *
 * <p>Implementations must treat source excerpts as data, never as executable instructions.
 */
public interface ChatGenerationPort {

    void stream(GroundedChatRequest request, Listener listener);

    record GroundedChatRequest(String systemInstruction, String question, List<Source> sources) {

        public GroundedChatRequest {
            systemInstruction = requireText(systemInstruction, "systemInstruction");
            question = requireText(question, "question");
            sources = List.copyOf(Objects.requireNonNull(sources, "sources must not be null"));
        }

        private static String requireText(String value, String fieldName) {
            String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalized;
        }
    }

    record Source(String reference, Citation citation, String content) {

        public Source {
            reference = requireText(reference, "reference");
            citation = Objects.requireNonNull(citation, "citation must not be null");
            content = requireText(content, "content");
        }

        private static String requireText(String value, String fieldName) {
            String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalized;
        }
    }

    interface Listener {

        void onToken(String token);

        void onComplete();

        void onError(Throwable error);
    }
}
