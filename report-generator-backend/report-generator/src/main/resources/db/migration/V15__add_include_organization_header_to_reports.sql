-- =========================================================================
-- SwasthAI Report Generator
-- V15 - Add Include Organization Header to Reports
-- =========================================================================

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS include_organization_header BOOLEAN NOT NULL DEFAULT FALSE;