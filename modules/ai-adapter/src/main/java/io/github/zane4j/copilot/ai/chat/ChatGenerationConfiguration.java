package io.github.zane4j.copilot.ai.chat;

import io.github.zane4j.copilot.rag.ChatGenerationPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ChatGenerationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "copilot.chat", name = "provider", havingValue = "context", matchIfMissing = true)
    ChatGenerationPort contextOnlyChatGenerationPort() {
        return new ContextOnlyChatGenerationAdapter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "copilot.chat", name = "provider", havingValue = "openai")
    @ConditionalOnBean(ChatClient.Builder.class)
    ChatGenerationPort openAiChatGenerationPort(ChatClient.Builder builder) {
        return new SpringAiChatGenerationAdapter(builder);
    }
}
