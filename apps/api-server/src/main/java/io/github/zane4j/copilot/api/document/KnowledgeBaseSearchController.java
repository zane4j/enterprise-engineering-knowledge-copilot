package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.rag.RetrievalPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/search")
class KnowledgeBaseSearchController {

    private final KnowledgeBaseSearchService knowledgeBaseSearchService;

    KnowledgeBaseSearchController(KnowledgeBaseSearchService knowledgeBaseSearchService) {
        this.knowledgeBaseSearchService = knowledgeBaseSearchService;
    }

    @PostMapping
    SearchResponse search(
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody SearchRequest request) {
        List<SearchHitResponse> hits = knowledgeBaseSearchService
                .search(knowledgeBaseId, request.query(), request.limit())
                .stream()
                .map(this::toResponse)
                .toList();
        return new SearchResponse(hits);
    }

    private SearchHitResponse toResponse(RetrievalPort.RetrievedChunk retrieved) {
        return new SearchHitResponse(
                retrieved.content(),
                new CitationResponse(
                        retrieved.citation().documentId(),
                        retrieved.citation().documentName(),
                        retrieved.citation().locator(),
                        retrieved.citation().score(),
                        retrieved.citation().metadata()));
    }

    record SearchRequest(
            @NotBlank @Size(max = 2_000) String query,
            @Min(1) @Max(20) Integer limit) {
    }

    record SearchResponse(List<SearchHitResponse> hits) {
    }

    record SearchHitResponse(String content, CitationResponse citation) {
    }

    record CitationResponse(
            UUID documentId,
            String documentName,
            String locator,
            double score,
            Map<String, String> metadata) {
    }
}
