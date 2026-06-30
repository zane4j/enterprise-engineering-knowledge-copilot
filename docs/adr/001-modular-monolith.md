# ADR-001: Start with a modular monolith

## Status
Accepted

## Decision
Use Maven modules inside one deployable API application and one asynchronous worker process.

## Context
The first release needs clear ownership boundaries but has no evidence that independent deployment of user, document, retrieval, and chat services is required.

## Consequences
- Faster end-to-end delivery and easier local development.
- Module boundaries preserve a future extraction path.
- Kafka still separates the resource-intensive ingestion workflow.
- Cross-module dependencies must point inward toward domain abstractions.
