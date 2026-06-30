CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_claim
    ON ingestion_jobs (status, updated_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_version
    ON document_chunks (tenant_id, document_id, document_version, chunk_index);
