package io.github.zane4j.copilot.ai;

import io.github.zane4j.copilot.rag.embedding.EmbeddingPort;
import io.github.zane4j.copilot.rag.embedding.HashEmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "copilot.embedding", name = "provider", havingValue = "hash")
    EmbeddingPort hashEmbeddingPort(EmbeddingProperties properties) {
        return new HashEmbeddingPort(properties.dimensions());
    }

    @Bean
    @ConditionalOnProperty(prefix = "copilot.embedding", name = "provider", havingValue = "openai")
    EmbeddingPort openAiEmbeddingPort(EmbeddingModel embeddingModel, EmbeddingProperties properties) {
        return new SpringAiEmbeddingPort(embeddingModel, "openai:" + properties.model(), properties.dimensions());
    }
}
