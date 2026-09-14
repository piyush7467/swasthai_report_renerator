-- =========================================================================
-- SwasthAI Report Generator
-- V12 - Organization Profiles / Branding / Contact / Signature
-- =========================================================================

CREATE TABLE organization_profiles (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    logo_storage_key VARCHAR(500),

    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    phone VARCHAR(30),
    alternate_phone VARCHAR(30),
    email VARCHAR(150),
    website VARCHAR(255),

    signature_storage_key VARCHAR(500),

    report_footer_text VARCHAR(1000),
    report_disclaimer VARCHAR(2000),

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_organization_profile_organization
        UNIQUE (organization_id),

    CONSTRAINT fk_organization_profile_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_organization_profiles_organization
    ON organization_profiles(organization_id);