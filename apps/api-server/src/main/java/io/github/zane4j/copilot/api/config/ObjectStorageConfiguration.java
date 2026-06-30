package io.github.zane4j.copilot.api.config;

import io.github.zane4j.copilot.storage.MinioObjectStorageAdapter;
import io.github.zane4j.copilot.storage.ObjectStoragePort;
import io.minio.MinioClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
class ObjectStorageConfiguration {

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
}
