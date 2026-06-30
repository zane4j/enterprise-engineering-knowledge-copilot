# Ingestion Pipeline

## Current executable flow

```text
Upload API
  -> MinIO original object
  -> documents: PENDING
  -> ingestion_jobs: PENDING
  -> polling worker claims one job
  -> MinIO read
  -> Markdown/TXT parser
  -> header-aware chunker
  -> document_chunks persistence
  -> documents: READY, ingestion_jobs: SUCCEEDED
```

## Reliability behavior

- Job claim uses `FOR UPDATE SKIP LOCKED`, so multiple worker instances do not process the same job concurrently.
- A `PROCESSING` job becomes eligible for reclaim after `stale-processing-after-seconds`.
- Processing failures return a job to `PENDING` until `max-attempts` is reached.
- Unsupported or malformed content is terminal and sets document/job status to `FAILED`.
- Before inserting chunks, the worker deletes existing chunks for the same document version. This makes retry persistence idempotent.

## Supported formats

| Format | Current state |
|---|---|
| TXT | Parsed and chunked |
| Markdown | Parsed by headings and chunked |
| PDF | Accepted by upload API but not parsed yet; worker marks it `FAILED` with a clear reason |

## Chunk metadata

Every persisted chunk stores the original file name, content type, section title, source start line, chunk index, and character count in `metadata`.

## Deliberate limitation

The current worker persists `document_chunks` and marks a document `READY`, but the `embedding` column remains null until the next increment introduces a configured Embedding Provider. Therefore the project is not yet able to perform semantic vector search.
