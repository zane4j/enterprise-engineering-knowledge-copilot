# Identity Provider Claim Contract

Configure your OIDC-compatible identity provider so access tokens issued for this API include these claims:

| Claim | Type | Required | Meaning |
| --- | --- | --- | --- |
| `iss` | URL | Yes | Must match `COPILOT_JWT_ISSUER_URI`. |
| `sub` | String | Yes | Identity-provider subject; retained for provider auditability. |
| `exp` | Unix timestamp | Yes | Token expiration. |
| `tenant_id` | UUID string | Yes | The target application tenant. |
| `user_id` | UUID string | Yes | The application `users.id` value for the authenticated person. |

Example decoded access-token payload:

```json
{
  "iss": "https://identity.example.com/realms/engineering",
  "sub": "idp-user-12345",
  "exp": 1893456000,
  "tenant_id": "11111111-1111-1111-1111-111111111111",
  "user_id": "22222222-2222-2222-2222-222222222222"
}
```

Do not accept tenant or user identity from an HTTP request body, a browser header, or URL query parameters. The identity provider must add claims server-side after authenticating the user.

## Token audience

When your identity provider supports audiences, issue tokens specifically for this API. Audience validation is a recommended next hardening step; the initial resource-server configuration validates issuer and signature, while application authorization validates the user/tenant database mapping and knowledge-base membership.
