package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.rag.chat.GroundedEvidence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/chat")
class GroundedChatController {

    private static final long NO_TIMEOUT = 0L;

    private final GroundedChatService groundedChatService;

    GroundedChatController(GroundedChatService groundedChatService) {
        this.groundedChatService = groundedChatService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody ChatRequest request) {
        GroundedChatService.PreparedGroundedChat prepared =
                groundedChatService.prepare(knowledgeBaseId, request.question(), request.maxResults());
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        emitter.onCompletion(() -> dispose(subscription));
        emitter.onTimeout(() -> dispose(subscription));

        try {
            for (GroundedEvidence evidence : prepared.evidence()) {
                emitter.send(SseEmitter.event()
                        .name("citation")
                        .id(evidence.sourceId())
                        .data(new CitationEvent(
                                evidence.sourceId(),
                                evidence.citation().documentId(),
                                evidence.citation().documentName(),
                                evidence.citation().locator(),
                                evidence.citation().score(),
                                evidence.citation().metadata())));
            }
        } catch (IOException exception) {
            emitter.complete();
            return emitter;
        }

        Disposable disposable = prepared.tokens().subscribe(
                token -> sendToken(emitter, token),
                error -> completeWithError(emitter),
                () -> complete(emitter, prepared.evidence().size(), prepared.grounded()));
        subscription.set(disposable);
        return emitter;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().name("token").data(new TokenEvent(token)));
        } catch (IOException exception) {
            emitter.complete();
        }
    }

    private void completeWithError(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(Map.of("code", "CHAT_PROVIDER_FAILED", "message", "The chat provider could not complete this response.")));
        } catch (IOException ignored) {
            // The client may already be disconnected.
        } finally {
            emitter.complete();
        }
    }

    private void complete(SseEmitter emitter, int citationCount, boolean grounded) {
        try {
            emitter.send(SseEmitter.event().name("completed")
                    .data(new CompletedEvent(citationCount, grounded)));
        } catch (IOException ignored) {
            // The client may already be disconnected.
        } finally {
            emitter.complete();
        }
    }

    private void dispose(AtomicReference<Disposable> subscription) {
        Disposable disposable = subscription.getAndSet(null);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    record ChatRequest(
            @NotBlank @Size(max = 2_000) String question,
            @Min(1) @Max(10) Integer maxResults) {
    }

    record TokenEvent(String text) {
    }

    record CitationEvent(
            String sourceId,
            UUID documentId,
            String documentName,
            String locator,
            double score,
            Map<String, String> metadata) {
    }

    record CompletedEvent(int citationCount, boolean grounded) {
    }
}
