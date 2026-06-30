# Embedding and Vector Retrieval

## Scope

This increment creates the first semantic retrieval path:

```text
Chunk content
  -> EmbeddingPort
  -> pgvector document_chunks.embedding
  -> tenant- and knowledge-base-filtered cosine search
  -> source-grounded retrieval response
```

## Provider policy

The database schema uses `vector(1536)`, so all configured providers must return 1536-dimensional vectors.

### Local development

```properties
COPILOT_EMBEDDING_PROVIDER=hash
SPRING_AI_EMBEDDING_MODEL=none
```

The hash provider is deterministic and allows the entire pipeline to run without an API key. It exists only for local development, demonstrations, and automated tests; it is not semantically comparable to a production embedding model.

### Production OpenAI configuration

```properties
COPILOT_EMBEDDING_PROVIDER=openai
SPRING_AI_EMBEDDING_MODEL=openai
OPENAI_API_KEY=<secret>
COPILOT_EMBEDDING_MODEL=text-embedding-3-small
COPILOT_EMBEDDING_DIMENSIONS=1536
```

OpenAI credentials must be supplied through server environment variables or a secret manager. Do not commit API keys.

## Ingestion behavior

The worker embeds all chunks in bounded batches before persistence. A document is `READY` only after chunk and embedding writes succeed in the same database transaction.

Each chunk metadata entry includes:

- source file name and content type
- Markdown section title and start line
- chunk index and character count
- embedding provider and dimension

## Retrieval API

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/search`

```json
{
  "query": "How do I troubleshoot database connection pool exhaustion?",
  "limit": 5
}
```

The API derives the tenant from the authenticated actor, verifies knowledge-base read access, embeds the query, and executes cosine-distance search with both `tenant_id` and `knowledge_base_id` predicates. It returns chunk content and source metadata; it does not call a chat model yet.

## Reindexing

Documents uploaded before embeddings were introduced can be reprocessed:

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`

Reindexing increments the document version, creates a new durable ingestion job, and replaces all previous chunks only after the new chunk/embedding transaction succeeds.
