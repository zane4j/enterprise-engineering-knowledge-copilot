package io.github.zane4j.copilot.api;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
class SystemController {

    @GetMapping("/info")
    ResponseEntity<SystemInfoResponse> info() {
        return ResponseEntity.ok(new SystemInfoResponse(
                "enterprise-engineering-knowledge-copilot",
                "0.1.0-SNAPSHOT",
                "FOUNDATION",
                Instant.now()));
    }

    record SystemInfoResponse(String service, String version, String milestone, Instant timestamp) {
    }
}
