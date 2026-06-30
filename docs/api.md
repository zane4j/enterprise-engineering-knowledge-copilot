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

The response includes matching chunks and citations.

## Grounded chat stream

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat/stream`

Request:

```json
{
  "question": "How should I troubleshoot database connection pool exhaustion?",
  "maxResults": 5
}
```

Response content type: `text/event-stream`

```text
event: citation
data: {"sourceId":"S1","documentName":"payment-runbook.md",...}

event: token
data: {"text":"Check the active connection count..."}

event: completed
data: {"citationCount":1,"grounded":true}
```

The service performs retrieval before model invocation. It emits frozen source citations before token events. When no evidence is retrieved, it emits a direct knowledge-gap answer and does not call the chat model.

## Planned endpoints

- `POST /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases`
- `POST /api/v1/chat/sessions`
- `POST /api/v1/incidents/analyze`
- `POST /api/v1/feedback`
