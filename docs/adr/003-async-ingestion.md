# ADR-003: Process document ingestion asynchronously

## Status
Accepted

## Decision
Persist an ingestion job and publish an event after document upload. A worker performs parsing, chunking, embedding, and persistence.

## Context
Parsing and embedding can be slow, can fail transiently, and should not hold an HTTP request open.

## Consequences
- The upload endpoint returns quickly with a trackable job status.
- Jobs can retry with bounded attempts and a dead-letter path.
- Chat uses only documents in `READY` state.
- Event payloads must contain tenant and document identifiers; background processing must not use an HTTP ThreadLocal context.
