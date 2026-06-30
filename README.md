# Enterprise Engineering Knowledge Copilot

A multi-tenant, source-grounded RAG platform for engineering teams.

It ingests internal engineering documents such as runbooks, architecture decisions, incident postmortems, API specifications, and SOPs. Users can ask questions within their authorized knowledge bases and receive streaming answers with traceable citations.

## Core capabilities

- Tenant and knowledge-base isolation
- RBAC-enforced retrieval
- Asynchronous document ingestion
- Hybrid retrieval: vector + keyword search
- Source citations and low-confidence handling
- Incident troubleshooting workflows
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

## Delivery roadmap

1. **Foundation**: project skeleton, local Docker infrastructure, architecture docs, CI.
2. **RAG MVP**: upload documents, parse/chunk/embed, persist vectors, retrieve, answer with citations.
3. **Enterprise controls**: JWT, tenant isolation, RBAC, Kafka ingestion jobs, audit logs, streaming responses.
4. **Quality and operations**: hybrid retrieval, reranking, incident copilot, evaluation suite, dashboards.

## License

MIT
