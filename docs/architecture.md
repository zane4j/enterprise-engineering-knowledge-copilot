# Architecture

## Goal

Provide a secure, tenant-isolated RAG platform for engineering teams. The system grounds answers in approved internal sources such as runbooks, ADRs, incident postmortems, API specifications, and SOPs.

## Runtime components

```text
React Web
  -> Spring Boot API
      -> PostgreSQL + pgvector: metadata, chunks, keyword index, vectors
      -> Redis: session, cache, rate limiting
      -> MinIO: original document objects
      -> Kafka: ingestion events and retry workflow
  -> Ingestion Worker
      -> document reader -> cleaner -> header-aware chunker -> embedding provider -> PostgreSQL
```

## Request flow

1. The API authenticates the request and derives tenant and user identity from JWT claims.
2. The authorization layer verifies knowledge-base membership and role.
3. The chat service executes tenant- and knowledge-base-filtered retrieval.
4. Retrieved chunks are fused, reranked, deduplicated, and passed to the LLM with source identifiers.
5. The API streams tokens and citations through SSE.
6. The application stores an audit record without logging raw sensitive content by default.

## Ingestion flow

1. The API validates file type, size, and access rights.
2. The original object is written to MinIO.
3. A `PENDING` document and ingestion job are saved in PostgreSQL.
4. The API publishes an ingestion event.
5. The worker parses, cleans, chunks, embeds, and persists the document.
6. The document is marked `READY` only after chunks and vectors are durable.

## Design constraints

- Tenant identity comes from authentication, never from an untrusted request field.
- Every retrieval query includes tenant and knowledge-base predicates.
- Only `READY` document versions are eligible for retrieval.
- The LLM must be instructed to state uncertainty when sources are insufficient.
- Background messages carry tenant identifiers explicitly; they do not rely on HTTP request context.
