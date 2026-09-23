-- =========================================================================
-- SwasthAI Report Generator
-- V18 - Create Report Shares Table for Secure Patient & Doctor Sharing
-- =========================================================================

CREATE TABLE IF NOT EXISTS report_shares (
    id UUID PRIMARY KEY,
    ref_id VARCHAR(40) NOT NULL UNIQUE,
    share_token VARCHAR(64) NOT NULL UNIQUE,
    report_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    report_ref_id VARCHAR(30) NOT NULL,
    shared_by_user_id UUID NOT NULL,
    share_channel VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    access_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_report_shares_report
        FOREIGN KEY (report_id)
        REFERENCES reports(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_report_shares_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_report_shares_user
        FOREIGN KEY (shared_by_user_id)
        REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_report_shares_token
    ON report_shares (share_token);

CREATE INDEX IF NOT EXISTS idx_report_shares_report
    ON report_shares (report_id);

CREATE INDEX IF NOT EXISTS idx_report_shares_organization
    ON report_shares (organization_id);

CREATE INDEX IF NOT EXISTS idx_report_shares_created_at
    ON report_shares (created_at DESC);
