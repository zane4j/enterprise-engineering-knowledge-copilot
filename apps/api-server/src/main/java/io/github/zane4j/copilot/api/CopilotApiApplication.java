package io.github.zane4j.copilot.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.zane4j.copilot")
public class CopilotApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopilotApiApplication.class, args);
    }
}
