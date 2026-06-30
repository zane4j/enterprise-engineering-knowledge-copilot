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

## Source-grounded streaming chat

`POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat/stream`

- Request: `application/json`
- Response: `text/event-stream`
- The API checks knowledge-base read access, retrieves tenant-scoped chunks, emits citations before model tokens, and refuses to synthesize when no sources are found.

```json
{
  "question": "How should I troubleshoot database connection pool exhaustion?"
}
```

The stream uses these event types:

| Event | Payload | Meaning |
| --- | --- | --- |
| `retrieval` | `citationCount` | Retrieval finished before generation starts. |
| `citation` | source reference and source metadata | A source available to support the answer, for example `S1`. |
| `token` | `text` | A streamed answer fragment. |
| `completed` | answer character count and citation count | Generation completed successfully. |
| `error` | safe error code and message | Generation failed without exposing provider details. |

Example with curl:

```bash
curl -N \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question":"Why use a modular monolith?"}' \
  http://localhost:8080/api/v1/knowledge-bases/00000000-0000-0000-0000-000000000010/chat/stream
```

Local mode uses `COPILOT_CHAT_PROVIDER=context` and streams grounded source excerpts without an external chat model. For model synthesis, set `COPILOT_CHAT_PROVIDER=openai`, `SPRING_AI_CHAT_MODEL=openai`, and `OPENAI_API_KEY`.

## Planned endpoints

- `POST /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases`
- `POST /api/v1/incidents/analyze`
- `POST /api/v1/feedback`
