-- =========================================================================
-- SwasthAI Report Generator
-- V23 - Add Signature Verification Lifecycle to Organization Profiles
-- =========================================================================

ALTER TABLE organization_profiles
    ADD COLUMN IF NOT EXISTS signature_verification_status VARCHAR(30) DEFAULT 'NOT_CONFIGURED',
    ADD COLUMN IF NOT EXISTS signature_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS signature_verified_by VARCHAR(150),
    ADD COLUMN IF NOT EXISTS signature_rejection_reason VARCHAR(500);

-- Existing uploaded signatures are considered approved so existing organizations are not disrupted
UPDATE organization_profiles
SET signature_verification_status = 'APPROVED'
WHERE signature_storage_key IS NOT NULL
  AND (signature_verification_status IS NULL OR signature_verification_status = 'NOT_CONFIGURED');
