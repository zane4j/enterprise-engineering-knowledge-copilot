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

## Authentication profiles

### Local profile

The `local` profile deliberately uses deterministic seed data and a fixed development actor. It has no JWT requirement so the full ingestion, retrieval, and chat flow can run offline. It must never be exposed as a public or shared environment.

### Production profile

The `prod` profile is a Spring Security OAuth2 resource server. It requires a trusted issuer:

```text
COPILOT_JWT_ISSUER_URI=https://identity.example.com/realms/engineering
```

The application starts in production only when this issuer configuration is present. Every API request other than health/info must include a valid Bearer JWT. Spring Security validates issuer, signature, and standard time claims before application code resolves the actor.

The token must include these UUID claims by default:

```json
{
  "tenant_id": "a tenant UUID from the local tenants table",
  "user_id": "a user UUID from the local users table"
}
```

Use `COPILOT_JWT_TENANT_CLAIM` and `COPILOT_JWT_USER_CLAIM` when the identity provider uses different claim names.

The API verifies that `users.id = user_id` belongs to `users.tenant_id = tenant_id` before the identity is used. JWT claims establish identity; database membership remains the source of truth for knowledge-base roles.

## Identity provisioning

Before a production user can access a knowledge base, provision matching rows in the application database and configure the identity provider to emit the same UUIDs as JWT claims:

```sql
INSERT INTO tenants (id, name)
VALUES ('<tenant-uuid>', 'Engineering')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, tenant_id, email, display_name)
VALUES ('<user-uuid>', '<tenant-uuid>', 'engineer@example.com', 'Engineer')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_base_members (knowledge_base_id, user_id, role)
VALUES ('<knowledge-base-uuid>', '<user-uuid>', 'OWNER')
ON CONFLICT (knowledge_base_id, user_id) DO UPDATE SET role = EXCLUDED.role;
```

Use a restricted administrative database path for this bootstrap. Do not expose generic user or role management endpoints until their audit trail and authorization model are implemented.

## Non-negotiable isolation rules

1. `tenantId` is derived from a verified authenticated principal, never from a request body, path, header, or query parameter.
2. Every document, chunk, job, chat session, and citation stores `tenant_id`.
3. Every retrieval query filters by `tenant_id` and `knowledge_base_id`.
4. Knowledge-base membership is checked before every read, write, search, or chat operation.
5. Document access is checked before a signed MinIO download URL is created.
6. Background jobs carry tenant context in persisted job records.
7. Audit logs record actions and identifiers; raw prompts and documents are excluded by default.
8. Secrets, private keys, and JWTs are never committed, logged, or pasted into chat.

## Database defense in depth

The schema keeps tenant identifiers on all sensitive records. A later hardening increment will add PostgreSQL Row-Level Security policies and integration tests proving that cross-tenant search, citation, and file access fail.
