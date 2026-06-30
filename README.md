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
Kafka
    |
    v
Ingestion Worker
    |-- document parsing
    |-- cleaning and chunking
    |-- embedding generation
    `-- vector persistence
```

## Technology stack

- Java 21, Spring Boot, Spring AI
- PostgreSQL 16 + pgvector
- Kafka, Redis, MinIO
- React, TypeScript, Vite
- Docker Compose
- Micrometer, OpenTelemetry, Prometheus, Grafana
- JUnit 5, Testcontainers

## Modules

```text
apps/
  api-server/          REST API, authentication, RAG chat
  ingestion-worker/    asynchronous document ingestion
modules/
  common/              shared primitives and error handling
  domain/              domain model and ports
  rag-core/            chunking, retrieval, citations
  security/            tenant context and authorization
  storage/             object storage and persistence adapters
infra/                 local infrastructure and configuration
docs/                  architecture decisions and API documentation
evaluation/            golden datasets and RAG evaluation assets
```

## Local development

### Start infrastructure

```bash
cd infra
docker compose up -d
docker compose --profile messaging up -d
```

### Run the API skeleton

```bash
mvn clean verify
mvn -pl apps/api-server spring-boot:run
curl http://localhost:8080/api/v1/system/info
```

## Current milestone: Foundation

- [x] Maven multi-module structure
- [x] Spring Boot API and ingestion-worker skeletons
- [x] PostgreSQL/pgvector, Redis, MinIO, and optional Kafka local environment
- [x] Initial schema, architecture, security model, ADRs, CI workflow
- [ ] Document upload and MinIO adapter
- [ ] Async ingestion and Kafka event flow
- [ ] Chunking, embeddings, pgvector retrieval
- [ ] JWT/RBAC, streaming chat, citations

## Project roadmap

1. **RAG MVP**: upload, parse, chunk, embed, store, retrieve, cite.
2. **Enterprise controls**: JWT, tenant isolation, RBAC, audit trails, async jobs.
3. **Quality and operations**: hybrid retrieval, reranking, RAG evaluation, telemetry.
4. **Incident Copilot**: source-grounded operational troubleshooting.

## License

MIT
