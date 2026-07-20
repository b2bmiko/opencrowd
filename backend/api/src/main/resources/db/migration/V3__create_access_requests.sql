-- Access Requests table for the request workflow
-- Applied to tenant schema (Flyway runs against the tenant schema via search_path)

CREATE TABLE IF NOT EXISTS access_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requestor_name  VARCHAR(255) NOT NULL,
    requestor_email VARCHAR(255),
    requestor_user_id UUID,
    request_type    VARCHAR(20) NOT NULL DEFAULT 'ACCESS',
    application     VARCHAR(100) NOT NULL DEFAULT 'xwiki',
    resource_name   VARCHAR(255) NOT NULL DEFAULT '(global)',
    permission      VARCHAR(100) NOT NULL,
    justification   TEXT,
    custom_fields   TEXT, -- JSON for custom form fields
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_name   VARCHAR(255),
    reviewer_id     UUID,
    reviewed_at     TIMESTAMPTZ,
    review_comment  TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE INDEX IF NOT EXISTS idx_access_requests_status ON access_requests(status);
CREATE INDEX IF NOT EXISTS idx_access_requests_requestor ON access_requests(requestor_name);
CREATE INDEX IF NOT EXISTS idx_access_requests_created ON access_requests(created_at DESC);
