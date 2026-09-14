-- =========================================================================
-- SwasthAI Report Generator
-- V14 - Add Signature Owner Ref ID to Organization Profiles
-- =========================================================================

ALTER TABLE organization_profiles
    ADD COLUMN IF NOT EXISTS signature_owner_ref_id VARCHAR(100);
