package io.github.zane4j.copilot.ai;

import io.github.zane4j.copilot.rag.chat.GroundedChatPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class GroundedChatConfiguration {

    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    @ConditionalOnProperty(prefix = "copilot.chat", name = "provider", havingValue = "openai")
    GroundedChatPort springAiGroundedChatPort(ChatClient.Builder chatClientBuilder) {
        return new SpringAiGroundedChatPort(chatClientBuilder.build());
    }
}
