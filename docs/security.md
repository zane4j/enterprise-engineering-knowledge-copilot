# Security Model

## Authorization

The project uses tenant-scoped RBAC:

```text
Tenant
  -> Knowledge Base
      -> OWNER | ADMIN | EDITOR | VIEWER
```

- **OWNER**: full lifecycle and membership management.
- **ADMIN**: document and member management.
- **EDITOR**: upload and manage documents.
- **VIEWER**: query approved knowledge only.

## Non-negotiable isolation rules

1. `tenantId` is derived from an authenticated principal, not a request body or query parameter.
2. Every document, chunk, job, chat session, and citation stores `tenant_id`.
3. Every retrieval query filters by `tenant_id` and `knowledge_base_id`.
4. Document access is checked before a signed MinIO download URL is created.
5. Background jobs include tenant context in their event payload.
6. Audit logs record actions and identifiers; raw prompts and documents are excluded by default.

## Database defense in depth

The schema keeps tenant identifiers on all sensitive records. Phase 2 will add PostgreSQL Row-Level Security policies and integration tests proving that cross-tenant search, citation, and file access fail.
