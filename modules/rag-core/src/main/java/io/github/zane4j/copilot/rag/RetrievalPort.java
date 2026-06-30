package io.github.zane4j.copilot.rag;

import io.github.zane4j.copilot.domain.TenantId;
import java.util.List;
import java.util.UUID;

/**
 * Retrieval is explicitly tenant and knowledge-base scoped. Implementations must not infer either
 * value from untrusted request fields.
 */
public interface RetrievalPort {

    List<RetrievedChunk> search(SearchRequest request);

    record SearchRequest(TenantId tenantId, UUID knowledgeBaseId, String query, int limit) {
    }

    record RetrievedChunk(String content, Citation citation) {
    }
}
