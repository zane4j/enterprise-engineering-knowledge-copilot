# API Sketch

All production endpoints will be tenant-scoped through authenticated JWT claims. During the local profile, a deterministic seeded user is used only to allow end-to-end development without an identity provider.

## System

- `GET /api/v1/system/info` - foundation service metadata

## Document ingestion

### Upload a document

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents`

- Request: `multipart/form-data`, field name `file`
- Accepted extensions: `.pdf`, `.md`, `.markdown`, `.txt`
- Maximum file size: 25 MB by default
- Response: `202 Accepted`

```json
{
  "documentId": "3c5d8a98-e326-4fce-9178-8b09561a7d55",
  "ingestionJobId": "44afc73c-d8be-4cf8-a381-4e24a2c64c8a",
  "status": "PENDING"
}
```

The worker transitions a job through `PENDING -> PROCESSING -> SUCCEEDED | FAILED`. TXT and Markdown are parsed into metadata-rich chunks, embedded, and persisted in pgvector. PDF remains accepted at upload time but is not parsed yet.

### Reindex a document

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`

Returns `202 Accepted` and creates a new durable ingestion job. Use this after changing the embedding provider or for documents uploaded before the embedding feature.

### List documents

- `GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents`

### Inspect ingestion status

- `GET /api/v1/ingestion-jobs/{ingestionJobId}`

## Semantic retrieval

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/search`

```json
{
  "query": "How should I troubleshoot database connection pool exhaustion?",
  "limit": 5
}
```

```json
{
  "hits": [
    {
      "content": "# Database Connection Pool ...",
      "citation": {
        "documentId": "3c5d8a98-e326-4fce-9178-8b09561a7d55",
        "documentName": "payment-service-runbook.md",
        "locator": "Database Connection Pool (line 12, chunk 0)",
        "score": 0.82,
        "metadata": {
          "sectionTitle": "Database Connection Pool"
        }
      }
    }
  ]
}
```

## Planned endpoints

- `POST /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases`
- `POST /api/v1/chat/sessions`
- `POST /api/v1/chat/sessions/{sessionId}/messages/stream`
- `POST /api/v1/incidents/analyze`
- `POST /api/v1/feedback`

The future streaming chat endpoint will use Server-Sent Events and emit `token`, `citation`, `completed`, and `error` events.
