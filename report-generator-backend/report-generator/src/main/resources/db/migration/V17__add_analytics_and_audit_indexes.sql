-- =========================================================================
-- SwasthAI Report Generator
-- V17 - Add Analytics and Security Audit Performance Indexes
-- =========================================================================

-- Reports active queries index (for platform KPI totals and date range aggregation)
CREATE INDEX IF NOT EXISTS idx_reports_active_created_at
    ON reports (created_at DESC)
    WHERE deleted_at IS NULL;

-- Reports active status index (for draft / calculated / finalized breakdowns)
CREATE INDEX IF NOT EXISTS idx_reports_active_status
    ON reports (status)
    WHERE deleted_at IS NULL;

-- Reports active organization index (for tenant-level aggregation)
CREATE INDEX IF NOT EXISTS idx_reports_active_org_created
    ON reports (organization_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Security audit logs composite lookup by organization and timestamp
CREATE INDEX IF NOT EXISTS idx_security_audit_logs_org_created
    ON security_audit_logs (target_organization_ref_id, created_at DESC);

-- Security audit logs composite lookup by action and timestamp
CREATE INDEX IF NOT EXISTS idx_security_audit_logs_action_created
    ON security_audit_logs (action, created_at DESC);
