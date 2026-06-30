# ADR-002: Use PostgreSQL and pgvector

## Status
Accepted

## Decision
Use PostgreSQL as the system of record and pgvector for vector similarity search in the initial release.

## Context
The application needs transactional business data, tenant filters, keyword search, metadata filtering, and vector search. Operating separate relational and vector databases is not justified for the first release.

## Consequences
- A single backup, access-control, and migration boundary.
- SQL supports strict tenant and knowledge-base filters before retrieval.
- PostgreSQL full-text search can support hybrid retrieval.
- The design keeps a `RetrievalPort` so a dedicated search engine can be adopted later if scale requires it.
