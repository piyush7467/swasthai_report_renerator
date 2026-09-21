-- =========================================================================
-- SwasthAI Report Generator
-- V16 - Support License Deactivation and Resumption (Preserving Remaining Validity)
-- =========================================================================

-- 1. Update status check constraint to support DEACTIVATED
ALTER TABLE licenses
    DROP CONSTRAINT IF EXISTS chk_licenses_status;

ALTER TABLE licenses
    ADD CONSTRAINT chk_licenses_status
        CHECK (status IN ('ACTIVE', 'DEACTIVATED', 'EXPIRED'));

-- 2. Add remaining duration column to store frozen seconds when deactivated
ALTER TABLE licenses
    ADD COLUMN IF NOT EXISTS paused_remaining_seconds BIGINT;

-- 3. Add deactivated_at timestamp for audit tracking
ALTER TABLE licenses
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;
