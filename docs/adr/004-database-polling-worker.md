# ADR-004: Start worker dispatch with database polling

## Status
Accepted

## Decision
Use a database-polling worker with `FOR UPDATE SKIP LOCKED` for the first executable ingestion loop.

## Context
The project already persists durable ingestion jobs. A polling worker makes the upload-to-chunking path testable before Kafka topics, retries, and consumer groups are introduced. It avoids adding a transient message dependency before the job state machine is proven.

## Consequences
- Multiple workers can safely compete for work without double-claiming the same job.
- A stale `PROCESSING` job can be reclaimed after the configured timeout.
- All job transitions remain auditable in PostgreSQL.
- Kafka will replace dispatch only; parsing, chunking, and persistence interfaces remain reusable.
- Polling is not the final high-throughput design and will be replaced before production-scale ingestion.
