package io.github.zane4j.copilot.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CopilotIngestionWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopilotIngestionWorkerApplication.class, args);
    }
}
