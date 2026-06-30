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
Ingestion Worker
    |-- claim durable job
    |-- parse Markdown / TXT
    |-- header-aware chunking
    `-- persist chunks
```

## Technology stack

- Java 21, Spring Boot, Spring AI
- PostgreSQL 16 + pgvector
- Kafka, Redis, MinIO
- React, TypeScript, Vite
- Docker Compose
- Micrometer, OpenTelemetry, Prometheus, Grafana
- JUnit 5, Testcontainers

## Current milestone: Phase 1.5 — Executable text ingestion

- [x] Maven multi-module structure and CI
- [x] PostgreSQL/pgvector, Redis, MinIO, optional Kafka local environment
- [x] Initial tenant, knowledge-base, document, and ingestion-job schema
- [x] Document upload API for PDF, Markdown, and text files
- [x] MinIO-backed original-file storage with SHA-256 checksum
- [x] Durable `PENDING` ingestion job and document-status API
- [x] Database-polling worker with safe job claiming and stale-job recovery
- [x] Markdown/TXT parsing and header-aware chunking
- [x] Metadata-rich chunk persistence and retry-safe replacement
- [ ] PDF parsing
- [ ] Embedding provider and pgvector values
- [ ] Hybrid retrieval, streaming chat, citations, and evaluation
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

### 3. Upload a Markdown document

```bash
curl -i \
  -F "file=@docs/adr/001-modular-monolith.md;type=text/markdown" \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/documents
```

The API returns `202 Accepted`. Within the worker poll interval, the job should become `SUCCEEDED`, the document should become `READY`, and metadata-rich chunks should exist in PostgreSQL.

## Modules

```text
apps/
  api-server/          REST API, document upload, metadata persistence
  ingestion-worker/    durable job claim, text parsing, and chunk persistence
modules/
  common/              shared primitives and error handling
  domain/              domain model and ports
  rag-core/            document parsing, chunking, retrieval, citations
  security/            tenant context and authorization primitives
  storage/             MinIO/S3 object storage adapter
infra/                 local infrastructure and database bootstrap
docs/                  architecture decisions and API documentation
evaluation/            golden datasets and RAG evaluation assets
```

## License

MIT
