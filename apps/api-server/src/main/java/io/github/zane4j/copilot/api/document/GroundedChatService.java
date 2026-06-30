package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.api.security.CurrentActor;
import io.github.zane4j.copilot.api.security.CurrentActorProvider;
import io.github.zane4j.copilot.domain.TenantId;
import io.github.zane4j.copilot.rag.RetrievalPort;
import io.github.zane4j.copilot.rag.chat.ChatUnavailableException;
import io.github.zane4j.copilot.rag.chat.GroundedChatPort;
import io.github.zane4j.copilot.rag.chat.GroundedEvidence;
import io.github.zane4j.copilot.rag.chat.GroundedPrompt;
import io.github.zane4j.copilot.rag.chat.GroundedPromptBuilder;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@EnableConfigurationProperties(GroundedChatProperties.class)
class GroundedChatService {

    private static final int DEFAULT_MAX_RESULTS = 5;

    private final CurrentActorProvider currentActorProvider;
    private final KnowledgeBaseAccessRepository knowledgeBaseAccessRepository;
    private final RetrievalPort retrievalPort;
    private final ObjectProvider<GroundedChatPort> groundedChatPortProvider;
    private final GroundedChatProperties properties;
    private final GroundedPromptBuilder promptBuilder = new GroundedPromptBuilder();

    GroundedChatService(
            CurrentActorProvider currentActorProvider,
            KnowledgeBaseAccessRepository knowledgeBaseAccessRepository,
            RetrievalPort retrievalPort,
            ObjectProvider<GroundedChatPort> groundedChatPortProvider,
            GroundedChatProperties properties) {
        this.currentActorProvider = currentActorProvider;
        this.knowledgeBaseAccessRepository = knowledgeBaseAccessRepository;
        this.retrievalPort = retrievalPort;
        this.groundedChatPortProvider = groundedChatPortProvider;
        this.properties = properties;
    }

    PreparedGroundedChat prepare(UUID knowledgeBaseId, String question, Integer requestedMaxResults) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        knowledgeBaseAccessRepository.requireReadAccess(actor.tenantId(), actor.userId(), knowledgeBaseId);
        int maxResults = requestedMaxResults == null ? DEFAULT_MAX_RESULTS : requestedMaxResults;

        List<RetrievalPort.RetrievedChunk> retrieved = retrievalPort.search(new RetrievalPort.SearchRequest(
                new TenantId(actor.tenantId()), knowledgeBaseId, question, maxResults));
        if (retrieved.isEmpty()) {
            return new PreparedGroundedChat(
                    List.of(),
                    Flux.just("I don't have enough information in the selected knowledge base to answer confidently."),
                    false);
        }

        GroundedChatPort chatPort = groundedChatPortProvider.getIfAvailable();
        if (chatPort == null) {
            throw new ChatUnavailableException(
                    "Chat is not configured. Set COPILOT_CHAT_PROVIDER=openai and SPRING_AI_CHAT_MODEL=openai.");
        }

        List<GroundedEvidence> evidence = toEvidence(retrieved);
        GroundedPrompt prompt = promptBuilder.build(question, evidence, properties.maxEvidenceCharacters());
        return new PreparedGroundedChat(
                evidence,
                chatPort.stream(new GroundedChatPort.GroundedChatRequest(prompt.systemPrompt(), prompt.userPrompt())),
                true);
    }

    private List<GroundedEvidence> toEvidence(List<RetrievalPort.RetrievedChunk> retrieved) {
        java.util.ArrayList<GroundedEvidence> evidence = new java.util.ArrayList<>(retrieved.size());
        for (int index = 0; index < retrieved.size(); index++) {
            RetrievalPort.RetrievedChunk chunk = retrieved.get(index);
            evidence.add(new GroundedEvidence("S" + (index + 1), chunk.content(), chunk.citation()));
        }
        return List.copyOf(evidence);
    }

    record PreparedGroundedChat(
            List<GroundedEvidence> evidence,
            Flux<String> tokens,
            boolean grounded) {
    }
}
