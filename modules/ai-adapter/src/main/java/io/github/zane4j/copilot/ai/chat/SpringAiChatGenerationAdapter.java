package io.github.zane4j.copilot.ai.chat;

import io.github.zane4j.copilot.rag.ChatGenerationPort;
import org.springframework.ai.chat.client.ChatClient;

/** Spring AI-backed streaming chat adapter. */
final class SpringAiChatGenerationAdapter implements ChatGenerationPort {

    private final ChatClient chatClient;

    SpringAiChatGenerationAdapter(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public void stream(GroundedChatRequest request, Listener listener) {
        try {
            chatClient.prompt()
                    .system(system -> system
                            .text("{systemInstruction}")
                            .param("systemInstruction", request.systemInstruction()))
                    .user(user -> user
                            .text("{question}")
                            .param("question", request.question()))
                    .stream()
                    .content()
                    .subscribe(listener::onToken, listener::onError, listener::onComplete);
        } catch (RuntimeException exception) {
            listener.onError(exception);
        }
    }
}
