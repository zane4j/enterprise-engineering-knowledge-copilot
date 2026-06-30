INSERT INTO tenants (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'local-demo')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, tenant_id, email, display_name)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'demo@example.local',
    'Local Demo User'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_bases (id, tenant_id, name, description, created_by)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000001',
    'Engineering Runbooks',
    'Seed knowledge base for local document-ingestion development.',
    '00000000-0000-0000-0000-000000000002'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_base_members (knowledge_base_id, user_id, role)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000002',
    'OWNER'
)
ON CONFLICT (knowledge_base_id, user_id) DO NOTHING;
