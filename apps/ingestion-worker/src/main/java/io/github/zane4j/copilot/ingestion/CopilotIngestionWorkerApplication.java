package io.github.zane4j.copilot.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "io.github.zane4j.copilot")
public class CopilotIngestionWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopilotIngestionWorkerApplication.class, args);
    }
}
