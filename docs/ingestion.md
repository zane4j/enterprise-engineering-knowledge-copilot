# Ingestion Worker

## Current scope

This increment processes Markdown and plain-text documents that are already stored through the upload API.

```text
PENDING ingestion job
  -> claim with FOR UPDATE SKIP LOCKED
  -> read original object from MinIO
  -> parse UTF-8 Markdown/TXT
  -> clean and header-aware chunk
  -> persist document_chunks
  -> mark document READY and job SUCCEEDED
```

PDF parsing and embedding generation are intentionally outside this increment. PDF jobs fail with a clear `PDF_PARSING_NOT_IMPLEMENTED` reason until the next parser is added.

## Why polling first

Kafka will still be used later for event-driven ingestion. Polling the durable database queue first keeps the system reliable during early development:

- uploaded documents cannot be lost when Kafka is not running;
- jobs are visible and retryable from PostgreSQL;
- `FOR UPDATE SKIP LOCKED` allows multiple workers later;
- the same processing service can be called from a Kafka consumer later.

## Current limitations

- `document_chunks.embedding` is still null.
- Retrieval does not use these chunks yet.
- Chunking uses character windows, not model-specific tokenization.
- PDF, DOCX, HTML and Confluence exports are not parsed yet.
