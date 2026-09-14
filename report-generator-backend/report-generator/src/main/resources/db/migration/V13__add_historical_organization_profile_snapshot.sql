-- =========================================================================
-- SwasthAI Report Generator
-- V13 - Historical Organization Profile Snapshot
-- =========================================================================

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS organization_address_line1 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS organization_address_line2 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS organization_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS organization_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS organization_postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS organization_country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS organization_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS organization_alternate_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS organization_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS organization_website VARCHAR(255),
    ADD COLUMN IF NOT EXISTS organization_logo_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS organization_signature_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS organization_signature_owner_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS organization_signature_owner_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS organization_report_footer_text VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS organization_report_disclaimer VARCHAR(2000);
    