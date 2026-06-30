package io.github.zane4j.copilot.ingestion.config;

import io.github.zane4j.copilot.rag.ingestion.DocumentContentParser;
import io.github.zane4j.copilot.rag.ingestion.HeaderAwareChunker;
import io.github.zane4j.copilot.rag.ingestion.PlainTextAndMarkdownParser;
import io.github.zane4j.copilot.storage.MinioObjectStorageAdapter;
import io.github.zane4j.copilot.storage.ObjectStoragePort;
import io.minio.MinioClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({IngestionProperties.class, ObjectStorageProperties.class})
class IngestionWorkerConfiguration {

    @Bean
    MinioClient minioClient(ObjectStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    MinioObjectStorageAdapter minioObjectStorageAdapter(
            MinioClient minioClient,
            ObjectStorageProperties properties) {
        return new MinioObjectStorageAdapter(minioClient, properties.bucketName());
    }

    @Bean
    ObjectStoragePort objectStoragePort(MinioObjectStorageAdapter adapter) {
        return adapter;
    }

    @Bean
    ApplicationRunner initializeObjectStorageBucket(MinioObjectStorageAdapter adapter) {
        return arguments -> adapter.ensureBucketExists();
    }

    @Bean
    DocumentContentParser documentContentParser() {
        return new PlainTextAndMarkdownParser();
    }

    @Bean
    HeaderAwareChunker headerAwareChunker(IngestionProperties properties) {
        return new HeaderAwareChunker(properties.maxChunkCharacters(), properties.chunkOverlapCharacters());
    }
}
