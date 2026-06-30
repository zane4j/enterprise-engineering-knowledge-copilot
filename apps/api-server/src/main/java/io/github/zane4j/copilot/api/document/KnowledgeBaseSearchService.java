package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.api.security.CurrentActor;
import io.github.zane4j.copilot.api.security.CurrentActorProvider;
import io.github.zane4j.copilot.domain.TenantId;
import io.github.zane4j.copilot.rag.RetrievalPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class KnowledgeBaseSearchService {

    private static final int DEFAULT_LIMIT = 5;

    private final CurrentActorProvider currentActorProvider;
    private final KnowledgeBaseAccessRepository knowledgeBaseAccessRepository;
    private final RetrievalPort retrievalPort;

    KnowledgeBaseSearchService(
            CurrentActorProvider currentActorProvider,
            KnowledgeBaseAccessRepository knowledgeBaseAccessRepository,
            RetrievalPort retrievalPort) {
        this.currentActorProvider = currentActorProvider;
        this.knowledgeBaseAccessRepository = knowledgeBaseAccessRepository;
        this.retrievalPort = retrievalPort;
    }

    List<RetrievalPort.RetrievedChunk> search(UUID knowledgeBaseId, String query, Integer requestedLimit) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        knowledgeBaseAccessRepository.requireReadAccess(actor.tenantId(), actor.userId(), knowledgeBaseId);
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        return retrievalPort.search(new RetrievalPort.SearchRequest(
                new TenantId(actor.tenantId()), knowledgeBaseId, query, limit));
    }
}
