# API Sketch

All API endpoints are tenant-scoped through authenticated JWT claims. Clients must never supply a trusted `tenantId`.

## System

- `GET /api/v1/system/info` - foundation service metadata

## Planned endpoints

- `POST /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases`
- `POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents`
- `GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents`
- `POST /api/v1/chat/sessions`
- `POST /api/v1/chat/sessions/{sessionId}/messages/stream`
- `POST /api/v1/incidents/analyze`
- `POST /api/v1/feedback`

The streaming chat endpoint will use Server-Sent Events and emit `token`, `citation`, `completed`, and `error` events.
