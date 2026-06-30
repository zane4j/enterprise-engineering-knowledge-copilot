package io.github.zane4j.copilot.ai;

import io.github.zane4j.copilot.rag.chat.ChatProviderException;
import io.github.zane4j.copilot.rag.chat.GroundedChatPort;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

final class SpringAiGroundedChatPort implements GroundedChatPort {

    private final ChatClient chatClient;

    SpringAiGroundedChatPort(ChatClient chatClient) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
    }

    @Override
    public Flux<String> stream(GroundedChatRequest request) {
        try {
            return chatClient.prompt()
                    .system(request.systemPrompt())
                    .user(request.userPrompt())
                    .stream()
                    .content()
                    .onErrorMap(error -> new ChatProviderException("Chat provider stream failed", error));
        } catch (RuntimeException exception) {
            return Flux.error(new ChatProviderException("Chat provider request could not be created", exception));
        }
    }
}
