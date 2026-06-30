# Enterprise Engineering Knowledge Copilot

A multi-tenant, source-grounded RAG platform for engineering teams.

It ingests internal engineering documents such as runbooks, architecture decisions, incident postmortems, API specifications, and SOPs. Users can ask questions within their authorized knowledge bases and receive streaming answers with traceable citations.

## Why this project

Typical "chat with PDF" demos do not address the constraints that matter in an enterprise environment. This project focuses on:

- tenant and knowledge-base isolation
- RBAC-enforced retrieval
- asynchronous document ingestion
- hybrid retrieval: vector + keyword search
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
    |-- RAG Chat Orchestrator
    |-- Document Upload API
    |
    +-------------------------+
    |                         |
    v                         v
PostgreSQL + pgvector       MinIO
metadata / chunks / search  original files
    |
    v
Kafka / Worker
    |
    v
Parse -> Chunk -> Embed -> Persist vectors
```

## Technology stack

- Java 21, Spring Boot, Spring AI
- PostgreSQL 16 + pgvector
- Kafka, Redis, MinIO
- React, TypeScript, Vite
- Docker Compose
- Micrometer, OpenTelemetry, Prometheus, Grafana
- JUnit 5, Testcontainers

## Current milestone: Phase 1 — Document ingestion entry point

- [x] Maven multi-module structure and CI
- [x] PostgreSQL/pgvector, Redis, MinIO, optional Kafka local environment
- [x] Initial tenant, knowledge-base, document, and ingestion-job schema
- [x] Document upload API for PDF, Markdown, and text files
- [x] MinIO-backed original-file storage with SHA-256 checksum
- [x] Durable `PENDING` ingestion job and document-status API
- [x] Tenant/knowledge-base authorization boundary with local seeded development data
- [ ] Worker consumption, document parsing, chunking, and embeddings
- [ ] JWT authentication and production RBAC adapter
- [ ] Hybrid retrieval, streaming chat, citations, and evaluation

## Local development

### 1. Start infrastructure

```bash
cd infra
docker compose up -d
# Kafka is optional until the worker event consumer is enabled.
docker compose --profile messaging up -d
```

The local profile seeds this knowledge base:

```text
00000000-0000-0000-0000-000000000010  Engineering Runbooks
```

If you ran PostgreSQL before the seed script was added, reset local volumes once:

```bash
docker compose down -v
docker compose up -d
```

### 2. Run the API

```bash
mvn clean verify
mvn -pl apps/api-server spring-boot:run
```

### 3. Upload a sample document

```bash
curl -i \
  -F "file=@docs/adr/001-modular-monolith.md;type=text/markdown" \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/documents
```

The API returns `202 Accepted` with a durable ingestion job ID. The job remains `PENDING` until the parsing and embedding worker is implemented.

## Modules

```text
apps/
  api-server/          REST API, document upload, metadata persistence
  ingestion-worker/    upcoming parsing, chunking, embedding workflow
modules/
  common/              shared primitives and error handling
  domain/              domain model and ports
  rag-core/            chunking, retrieval, citations
  security/            tenant context and authorization primitives
  storage/             MinIO/S3 object storage adapter
infra/                 local infrastructure and database bootstrap
docs/                  architecture decisions and API documentation
evaluation/            golden datasets and RAG evaluation assets
```

## License

MIT
