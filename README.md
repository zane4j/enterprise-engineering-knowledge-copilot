# Enterprise Engineering Knowledge Copilot

A multi-tenant, source-grounded RAG platform for engineering teams.

It ingests internal engineering documents such as runbooks, architecture decisions, incident postmortems, API specifications, and SOPs. Users can search within their authorized knowledge bases and receive streaming, source-grounded answers with traceable citations.

## Why this project

Typical "chat with PDF" demos do not address the constraints that matter in an enterprise environment. This project focuses on:

- tenant and knowledge-base isolation
- RBAC-enforced retrieval
- asynchronous document ingestion
- provider-swappable embeddings and pgvector search
- source citations and low-confidence handling
- incident troubleshooting workflows
- RAG evaluation and observability

## Architecture

```text
React Web
    |
    | HTTPS / JWT / SSE
    v
Spring Boot API
    |-- Identity and RBAC
    |-- Knowledge Base Management
    |-- Vector Retrieval API
    |-- Grounded Chat + SSE
    |-- Document Upload API
    |
    +-------------------------+
    |                         |
    v                         v
PostgreSQL + pgvector       MinIO
metadata / chunks / vectors original files
    |
    v
Ingestion Worker
    |-- claim durable job
    |-- parse Markdown / TXT
    |-- header-aware chunking
    |-- batch embeddings
    `-- persist chunks and vectors
```

## Technology stack

- Java 21, Spring Boot, Spring AI
- PostgreSQL 16 + pgvector
- Kafka, Redis, MinIO
- React, TypeScript, Vite
- Docker Compose
- Micrometer, OpenTelemetry, Prometheus, Grafana
- JUnit 5, Testcontainers

## Current milestone: Phase 3 — Grounded streaming chat

- [x] Maven multi-module structure and CI
- [x] PostgreSQL/pgvector, Redis, MinIO, optional Kafka local environment
- [x] Tenant, knowledge-base, document, and ingestion-job schema
- [x] Document upload API and MinIO storage with SHA-256
- [x] Durable worker, Markdown/TXT parsing, and header-aware chunking
- [x] Local deterministic embedding provider for full offline development
- [x] OpenAI EmbeddingModel adapter through Spring AI configuration
- [x] 1536-dimensional pgvector persistence
- [x] Tenant- and knowledge-base-filtered cosine vector search API
- [x] Document reindex API for embedding changes or backfill
- [x] Source-grounded streaming chat endpoint with SSE citations
- [x] Local source-excerpt mode and Spring AI OpenAI chat adapter
- [ ] PDF parsing
- [ ] Hybrid retrieval and reranking
- [ ] JWT authentication and production RBAC adapter
- [ ] Persistent chat sessions, feedback, and evaluation
- [ ] Container image, HTTPS, and production deployment workflow

## Local development

### 1. Start infrastructure

```bash
cd infra
docker compose down -v
docker compose up -d
```

The local profile seeds this knowledge base:

```text
00000000-0000-0000-0000-000000000010  Engineering Runbooks
```

### 2. Run API and worker in separate terminals

```bash
mvn clean verify
mvn -pl apps/api-server spring-boot:run
mvn -pl apps/ingestion-worker spring-boot:run
```

### 3. Upload and chat with a Markdown document

```bash
curl -i \
  -F "file=@docs/adr/001-modular-monolith.md;type=text/markdown" \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/documents

curl -N \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question":"Why use a modular monolith?"}' \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/chat/stream
```

Local chat mode returns the retrieved evidence in SSE events. To enable model-generated synthesis, configure the OpenAI chat provider:

```bash
export COPILOT_CHAT_PROVIDER=openai
export SPRING_AI_CHAT_MODEL=openai
export OPENAI_API_KEY=replace-with-secret
```

## Modules

```text
apps/
  api-server/          REST API, retrieval, streaming chat, document metadata
  ingestion-worker/    durable job claim, parsing, chunking, embedding
modules/
  common/              shared primitives and error handling
  domain/              domain model and ports
  rag-core/            parsing, chunking, embeddings, retrieval and chat contracts
  ai-adapter/          Spring AI adapters and provider configuration
  security/            tenant context and authorization primitives
  storage/             MinIO/S3 object storage adapter
infra/                 local infrastructure and database bootstrap
docs/                  architecture decisions and API documentation
evaluation/            golden datasets and RAG evaluation assets
```

## License

MIT
