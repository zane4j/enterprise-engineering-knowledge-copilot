package io.github.zane4j.copilot.api.chat;

import io.github.zane4j.copilot.api.document.KnowledgeBaseSearchService;
import io.github.zane4j.copilot.rag.ChatGenerationPort;
import io.github.zane4j.copilot.rag.RetrievalPort;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
class GroundedChatService {

    private static final int RETRIEVAL_LIMIT = 6;

    private final KnowledgeBaseSearchService knowledgeBaseSearchService;
    private final ChatGenerationPort chatGenerationPort;
    private final GroundedPromptFactory promptFactory = new GroundedPromptFactory();

    GroundedChatService(
            KnowledgeBaseSearchService knowledgeBaseSearchService,
            ChatGenerationPort chatGenerationPort) {
        this.knowledgeBaseSearchService = knowledgeBaseSearchService;
        this.chatGenerationPort = chatGenerationPort;
    }

    void stream(UUID knowledgeBaseId, String question, EventListener eventListener) {
        try {
            List<RetrievalPort.RetrievedChunk> retrieved = knowledgeBaseSearchService.search(
                    knowledgeBaseId, question, RETRIEVAL_LIMIT);
            ChatGenerationPort.GroundedChatRequest prompt = promptFactory.create(question, retrieved);

            eventListener.onRetrieval(prompt.sources().size());
            prompt.sources().forEach(eventListener::onCitation);
            if (prompt.sources().isEmpty()) {
                String message = "I could not find enough information in this knowledge base to answer that question.";
                eventListener.onToken(message);
                eventListener.onCompleted(message.length(), 0);
                return;
            }

            AtomicInteger answerCharacterCount = new AtomicInteger();
            chatGenerationPort.stream(prompt, new ChatGenerationPort.Listener() {
                @Override
                public void onToken(String token) {
                    if (token == null || token.isEmpty()) {
                        return;
                    }
                    answerCharacterCount.addAndGet(token.length());
                    eventListener.onToken(token);
                }

                @Override
                public void onComplete() {
                    eventListener.onCompleted(answerCharacterCount.get(), prompt.sources().size());
                }

                @Override
                public void onError(Throwable error) {
                    eventListener.onError(error);
                }
            });
        } catch (RuntimeException exception) {
            eventListener.onError(exception);
        }
    }

    interface EventListener {

        void onRetrieval(int citationCount);

        void onCitation(ChatGenerationPort.Source source);

        void onToken(String token);

        void onCompleted(int answerCharacterCount, int citationCount);

        void onError(Throwable error);
    }
}
