# Enterprise Engineering Knowledge Copilot

A multi-tenant, source-grounded RAG platform for engineering teams.

It ingests internal engineering documents such as runbooks, architecture decisions, incident postmortems, API specifications, and SOPs. Users can search within their authorized knowledge bases and receive source-grounded streaming answers with traceable citations.

## Why this project

Typical "chat with PDF" demos do not address the constraints that matter in an enterprise environment. This project focuses on:

- tenant and knowledge-base isolation
- RBAC-enforced retrieval
- asynchronous document ingestion
- provider-swappable embeddings and pgvector search
- source citations frozen before generation
- streaming, source-grounded engineering answers
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
    |-- Vector Retrieval API
    |-- Grounded Chat + frozen citations
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
- [x] Provider-swappable embeddings and 1536-dimensional pgvector persistence
- [x] Tenant- and knowledge-base-filtered semantic retrieval
- [x] Grounded chat prompt with frozen citations and SSE response protocol
- [x] OpenAI ChatClient adapter through Spring AI configuration
- [ ] PDF parsing
- [ ] Hybrid retrieval and reranking
- [ ] Persisted chat sessions, feedback, and evaluation suite
- [ ] JWT authentication and production RBAC adapter
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

### 3. Upload and search a Markdown document

```bash
curl -i \
  -F "file=@docs/adr/001-modular-monolith.md;type=text/markdown" \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/documents

curl -s \
  -H "Content-Type: application/json" \
  -d '{"query":"Why use a modular monolith?","limit":5}' \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/search
```

### 4. Enable streaming chat

Chat is intentionally disabled by default. Enable it with a server-side API key:

```bash
export COPILOT_CHAT_PROVIDER=openai
export SPRING_AI_CHAT_MODEL=openai
export OPENAI_API_KEY=replace-with-secret
export COPILOT_CHAT_MODEL=gpt-5-mini
```

Then call:

```bash
curl -N \
  -H "Content-Type: application/json" \
  -d '{"question":"Why use a modular monolith?","maxResults":5}' \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/chat/stream
```

The endpoint emits `citation`, `token`, and `completed` events. If no evidence is retrieved, it returns a direct knowledge-gap response without calling the model.

## Modules

```text
apps/
  api-server/          REST API, retrieval, grounded chat, document metadata
  ingestion-worker/    durable job claim, parsing, chunking, embedding
modules/
  common/              shared primitives and error handling
  domain/              domain model and ports
  rag-core/            parsing, chunking, embedding and chat contracts
  ai-adapter/          Spring AI OpenAI adapters and configuration
  security/            tenant context and authorization primitives
  storage/             MinIO/S3 object storage adapter
infra/                 local infrastructure and database bootstrap
docs/                  architecture decisions and API documentation
evaluation/            golden datasets and RAG evaluation assets
```

## License

MIT
