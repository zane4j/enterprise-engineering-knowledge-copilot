package io.github.zane4j.copilot.rag;

import io.github.zane4j.copilot.domain.TenantId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Retrieval must always be tenant and knowledge-base scoped. */
public interface RetrievalPort {

    List<RetrievedChunk> search(SearchRequest request);

    record SearchRequest(TenantId tenantId, UUID knowledgeBaseId, String query, int limit) {

        public SearchRequest {
            Objects.requireNonNull(tenantId, "tenantId must not be null");
            Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
            query = Objects.requireNonNull(query, "query must not be null").trim();
            if (query.isEmpty()) {
                throw new IllegalArgumentException("query must not be blank");
            }
            if (limit < 1 || limit > 20) {
                throw new IllegalArgumentException("limit must be between 1 and 20");
            }
        }
    }

    record RetrievedChunk(String content, Citation citation) {

        public RetrievedChunk {
            content = Objects.requireNonNull(content, "content must not be null").trim();
            citation = Objects.requireNonNull(citation, "citation must not be null");
        }
    }
}
