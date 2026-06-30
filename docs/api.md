# API Sketch

All production endpoints will be tenant-scoped through authenticated JWT claims. During the local Phase 1 profile, a deterministic seeded user is used only to allow end-to-end development without an identity provider.

## System

- `GET /api/v1/system/info` - foundation service metadata

## Document ingestion: Phase 1

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

The upload request stores the original object in MinIO, persists document metadata, and creates a durable `PENDING` ingestion job in one database transaction. If database persistence fails after a successful object upload, the API attempts compensating object deletion.

The worker transitions a job through `PENDING -> PROCESSING -> SUCCEEDED | FAILED`. TXT and Markdown currently become metadata-rich chunks. PDF is accepted at upload time but remains unsupported by the worker until the PDF parser increment.

### List documents

- `GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents`

### Inspect ingestion status

- `GET /api/v1/ingestion-jobs/{ingestionJobId}`

## Planned endpoints

- `POST /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases`
- `POST /api/v1/chat/sessions`
- `POST /api/v1/chat/sessions/{sessionId}/messages/stream`
- `POST /api/v1/incidents/analyze`
- `POST /api/v1/feedback`

The future streaming chat endpoint will use Server-Sent Events and emit `token`, `citation`, `completed`, and `error` events.
