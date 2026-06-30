package io.github.zane4j.copilot.api.chat;

import io.github.zane4j.copilot.rag.ChatGenerationPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/chat")
class GroundedChatController {

    private final GroundedChatService groundedChatService;
    private final Executor chatStreamingExecutor;
    private final long timeoutMillis;

    GroundedChatController(
            GroundedChatService groundedChatService,
            @Qualifier("chatStreamingExecutor") Executor chatStreamingExecutor,
            @Value("${copilot.chat.sse-timeout-ms:90000}") long timeoutMillis) {
        this.groundedChatService = groundedChatService;
        this.chatStreamingExecutor = chatStreamingExecutor;
        this.timeoutMillis = timeoutMillis;
    }

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitter.onTimeout(emitter::complete);
        chatStreamingExecutor.execute(() -> groundedChatService.stream(
                knowledgeBaseId, request.question(), new SseEventListener(emitter)));
        return emitter;
    }

    record ChatRequest(@NotBlank @Size(max = 2_000) String question) {
    }

    private static final class SseEventListener implements GroundedChatService.EventListener {

        private final SseEmitter emitter;

        private SseEventListener(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onRetrieval(int citationCount) {
            send("retrieval", new RetrievalEvent(citationCount));
        }

        @Override
        public void onCitation(ChatGenerationPort.Source source) {
            send("citation", new CitationEvent(
                    source.reference(),
                    source.citation().documentId(),
                    source.citation().documentName(),
                    source.citation().locator(),
                    source.citation().score(),
                    source.citation().metadata()));
        }

        @Override
        public void onToken(String token) {
            send("token", new TokenEvent(token));
        }

        @Override
        public void onCompleted(int answerCharacterCount, int citationCount) {
            send("completed", new CompletedEvent(answerCharacterCount, citationCount));
            emitter.complete();
        }

        @Override
        public void onError(Throwable error) {
            try {
                send("error", new ErrorEvent("CHAT_GENERATION_FAILED", "Unable to generate a source-grounded answer"));
            } catch (RuntimeException ignored) {
                // The client may have closed the stream; do not leak internal error details.
            } finally {
                emitter.completeWithError(error);
            }
        }

        private void send(String eventName, Object body) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(body));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to send Server-Sent Event", exception);
            }
        }
    }

    record RetrievalEvent(int citationCount) {
    }

    record CitationEvent(
            String reference,
            UUID documentId,
            String documentName,
            String locator,
            double score,
            Map<String, String> metadata) {
    }

    record TokenEvent(String text) {
    }

    record CompletedEvent(int answerCharacterCount, int citationCount) {
    }

    record ErrorEvent(String code, String message) {
    }
}
