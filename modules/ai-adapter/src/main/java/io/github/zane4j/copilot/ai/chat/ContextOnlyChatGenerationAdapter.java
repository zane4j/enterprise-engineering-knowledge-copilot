package io.github.zane4j.copilot.ai.chat;

import io.github.zane4j.copilot.rag.ChatGenerationPort;

/**
 * Local development implementation that returns source excerpts without contacting an external chat model.
 *
 * <p>This makes the complete retrieval-to-SSE flow verifiable without an API key while avoiding simulated
 * model reasoning.
 */
final class ContextOnlyChatGenerationAdapter implements ChatGenerationPort {

    private static final int MAX_SOURCES = 3;
    private static final int MAX_EXCERPT_CHARS = 700;

    @Override
    public void stream(GroundedChatRequest request, Listener listener) {
        try {
            StringBuilder answer = new StringBuilder("Local source-grounded preview. The excerpts below are the retrieved evidence:\n\n");
            request.sources().stream().limit(MAX_SOURCES).forEach(source -> answer
                    .append('[').append(source.reference()).append("] ")
                    .append(compact(source.content()))
                    .append("\n\n"));
            answer.append("Configure COPILOT_CHAT_PROVIDER=openai and SPRING_AI_CHAT_MODEL=openai to enable model-generated synthesis.");

            emitInChunks(answer.toString(), listener);
            listener.onComplete();
        } catch (RuntimeException exception) {
            listener.onError(exception);
        }
    }

    private String compact(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_EXCERPT_CHARS
                ? normalized
                : normalized.substring(0, MAX_EXCERPT_CHARS).stripTrailing() + "…";
    }

    private void emitInChunks(String content, Listener listener) {
        int chunkSize = 180;
        for (int start = 0; start < content.length(); start += chunkSize) {
            listener.onToken(content.substring(start, Math.min(content.length(), start + chunkSize)));
        }
    }
}
