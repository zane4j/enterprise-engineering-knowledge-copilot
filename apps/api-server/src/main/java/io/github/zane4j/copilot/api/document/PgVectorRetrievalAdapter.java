package io.github.zane4j.copilot.api.document;

import io.github.zane4j.copilot.rag.Citation;
import io.github.zane4j.copilot.rag.RetrievalPort;
import io.github.zane4j.copilot.rag.embedding.EmbeddingException;
import io.github.zane4j.copilot.rag.embedding.EmbeddingPort;
import io.github.zane4j.copilot.rag.embedding.PgVectorLiteral;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PgVectorRetrievalAdapter implements RetrievalPort {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingPort embeddingPort;

    PgVectorRetrievalAdapter(JdbcTemplate jdbcTemplate, EmbeddingPort embeddingPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingPort = embeddingPort;
    }

    @Override
    public List<RetrievedChunk> search(SearchRequest request) {
        float[] queryEmbedding = embeddingPort.embed(request.query());
        String vectorLiteral = PgVectorLiteral.of(queryEmbedding);
        try {
            return jdbcTemplate.query("""
                    WITH query_embedding AS (SELECT ?::vector AS value)
                    SELECT chunk.document_id,
                           doc.original_file_name,
                           chunk.content,
                           COALESCE(chunk.metadata ->> 'sectionTitle', 'Document') AS section_title,
                           COALESCE((chunk.metadata ->> 'sectionStartLine')::integer, 1) AS section_start_line,
                           COALESCE((chunk.metadata ->> 'chunkIndex')::integer, 0) AS chunk_index,
                           1 - (chunk.embedding <=> query_embedding.value) AS score
                    FROM document_chunks chunk
                    JOIN documents doc
                      ON doc.id = chunk.document_id
                     AND doc.tenant_id = chunk.tenant_id
                    CROSS JOIN query_embedding
                    WHERE chunk.tenant_id = ?
                      AND chunk.knowledge_base_id = ?
                      AND doc.status = 'READY'
                      AND chunk.embedding IS NOT NULL
                    ORDER BY chunk.embedding <=> query_embedding.value
                    LIMIT ?
                    """, this::mapRetrievedChunk,
                    vectorLiteral,
                    request.tenantId().value(),
                    request.knowledgeBaseId(),
                    request.limit());
        } catch (DataAccessException exception) {
            throw new VectorSearchException("Vector retrieval failed", exception);
        }
    }

    private RetrievedChunk mapRetrievedChunk(ResultSet resultSet, int rowNum) throws SQLException {
        UUID documentId = resultSet.getObject("document_id", UUID.class);
        String documentName = resultSet.getString("original_file_name");
        String sectionTitle = resultSet.getString("section_title");
        int sectionStartLine = resultSet.getInt("section_start_line");
        int chunkIndex = resultSet.getInt("chunk_index");
        double score = resultSet.getDouble("score");
        String locator = "%s (line %d, chunk %d)".formatted(sectionTitle, sectionStartLine, chunkIndex);
        Citation citation = new Citation(
                documentId,
                documentName,
                locator,
                score,
                Map.of(
                        "sectionTitle", sectionTitle,
                        "sectionStartLine", Integer.toString(sectionStartLine),
                        "chunkIndex", Integer.toString(chunkIndex),
                        "embeddingProvider", embeddingPort.providerId()));
        return new RetrievedChunk(resultSet.getString("content"), citation);
    }
}
