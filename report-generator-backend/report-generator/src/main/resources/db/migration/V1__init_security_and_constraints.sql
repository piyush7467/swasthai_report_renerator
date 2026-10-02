-- =========================================================================
-- SwasthAI Report Generator - Base Schema & Security Invariants (V1)
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Organizations
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_organization_code UNIQUE (code),
    CONSTRAINT uk_organization_ref_id UNIQUE (ref_id)
);

CREATE INDEX IF NOT EXISTS idx_organization_status ON organizations (status);
CREATE INDEX IF NOT EXISTS idx_organization_ref_id ON organizations (ref_id);

-- -------------------------------------------------------------------------
-- 2. Organization Sequences
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organization_sequences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    patient_sequence BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sequence_organization UNIQUE (organization_id),
    CONSTRAINT fk_sequence_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 3. Users
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    organization_id UUID,
    last_login_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_user_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_user_organization ON users (organization_id);
CREATE INDEX IF NOT EXISTS idx_user_org_role ON users (organization_id, role);
CREATE INDEX IF NOT EXISTS idx_user_org_status ON users (organization_id, status);

-- -------------------------------------------------------------------------
-- 4. Password Reset Tokens
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_password_reset_token_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_user ON password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_expires_at ON password_reset_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_password_reset_used_at ON password_reset_tokens (used_at);

-- -------------------------------------------------------------------------
-- 5. Refresh Tokens
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rotated_at TIMESTAMPTZ,
    CONSTRAINT uk_refresh_token_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked_at ON refresh_tokens (revoked_at);

-- -------------------------------------------------------------------------
-- 6. Patients (Base Schema before V6 enhancements)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    organization_id UUID NOT NULL,
    patient_code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    address VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_patient_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_patient_org_patient_code UNIQUE (organization_id, patient_code),
    CONSTRAINT fk_patient_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_patient_organization ON patients (organization_id);
CREATE INDEX IF NOT EXISTS idx_patient_org_name ON patients (organization_id, name);
CREATE INDEX IF NOT EXISTS idx_patient_org_phone ON patients (organization_id, phone);

-- -------------------------------------------------------------------------
-- 7. Test Categories
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS test_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_test_category_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_test_category_code UNIQUE (code),
    CONSTRAINT uk_test_category_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_test_category_status ON test_categories (status);
CREATE INDEX IF NOT EXISTS idx_test_category_ref_id ON test_categories (ref_id);

-- -------------------------------------------------------------------------
-- 8. Tests
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    category_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    short_name VARCHAR(75),
    test_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    description VARCHAR(1000),
    sample_type VARCHAR(30) NOT NULL,
    custom_sample_type VARCHAR(100),
    specimen_container VARCHAR(150),
    sample_volume NUMERIC(10, 2),
    sample_volume_unit VARCHAR(20),
    fasting_required BOOLEAN NOT NULL DEFAULT FALSE,
    patient_preparation VARCHAR(1000),
    collection_instructions VARCHAR(1500),
    turnaround_time_hours INTEGER,
    priority_supported BOOLEAN NOT NULL DEFAULT FALSE,
    outsourced BOOLEAN NOT NULL DEFAULT FALSE,
    laboratory_instructions VARCHAR(1500),
    report_section VARCHAR(100),
    display_order INTEGER DEFAULT 0,
    report_description VARCHAR(1000),
    interpretation_guidance VARCHAR(2000),
    base_price NUMERIC(12, 2),
    currency VARCHAR(3) DEFAULT 'INR',
    billing_code VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER NOT NULL DEFAULT 1,
    effective_from DATE,
    effective_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_test_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_test_code UNIQUE (code),
    CONSTRAINT uk_test_name UNIQUE (name),
    CONSTRAINT fk_test_category FOREIGN KEY (category_id) REFERENCES test_categories (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_test_category ON tests (category_id);
CREATE INDEX IF NOT EXISTS idx_test_status ON tests (status);
CREATE INDEX IF NOT EXISTS idx_test_type ON tests (test_type);
CREATE INDEX IF NOT EXISTS idx_test_sample_type ON tests (sample_type);
CREATE INDEX IF NOT EXISTS idx_test_name ON tests (name);
CREATE INDEX IF NOT EXISTS idx_test_ref_id ON tests (ref_id);

-- -------------------------------------------------------------------------
-- 9. Test Parameters (Base Schema before V2 additions)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS test_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    test_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    data_type VARCHAR(20) NOT NULL,
    unit VARCHAR(50),
    required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 1,
    reference_min NUMERIC(19, 6),
    reference_max NUMERIC(19, 6),
    critical_low NUMERIC(19, 6),
    critical_high NUMERIC(19, 6),
    report_description VARCHAR(500),
    interpretation_guidance TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_test_parameter_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_test_parameter_test_code UNIQUE (test_id, code),
    CONSTRAINT fk_test_parameter_test FOREIGN KEY (test_id) REFERENCES tests (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_test_parameter_test ON test_parameters (test_id);
CREATE INDEX IF NOT EXISTS idx_test_parameter_status ON test_parameters (status);
CREATE INDEX IF NOT EXISTS idx_test_parameter_display_order ON test_parameters (test_id, display_order);
CREATE INDEX IF NOT EXISTS idx_test_parameter_ref_id ON test_parameters (ref_id);

-- -------------------------------------------------------------------------
-- 10. Organization Tests
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organization_tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id VARCHAR(30) NOT NULL,
    organization_id UUID NOT NULL,
    test_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    effective_from DATE,
    effective_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_org_test_ref_id UNIQUE (ref_id),
    CONSTRAINT uk_org_test UNIQUE (organization_id, test_id),
    CONSTRAINT fk_org_test_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_org_test_test FOREIGN KEY (test_id) REFERENCES tests (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_org_test_organization ON organization_tests (organization_id);
CREATE INDEX IF NOT EXISTS idx_org_test_test ON organization_tests (test_id);
CREATE INDEX IF NOT EXISTS idx_org_test_status ON organization_tests (status);
CREATE INDEX IF NOT EXISTS idx_org_test_effective_dates ON organization_tests (effective_from, effective_until);
CREATE INDEX IF NOT EXISTS idx_org_test_ref_id ON organization_tests (ref_id);

-- =========================================================================
-- Security & Database Invariants (Original V1 Invariants)
-- =========================================================================

-- 1. Enforce invariant: Only ONE active SUPER_ADMIN can exist in the platform
CREATE UNIQUE INDEX IF NOT EXISTS uk_single_super_admin
ON users (role)
WHERE role = 'SUPER_ADMIN';

-- 2. Enforce invariant: SUPER_ADMIN has organization_id IS NULL; ORG_ADMIN & LAB_STAFF must have organization_id
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_user_org_by_role') THEN
        ALTER TABLE users ADD CONSTRAINT chk_user_org_by_role CHECK (
            (role = 'SUPER_ADMIN' AND organization_id IS NULL) OR
            (role IN ('ORG_ADMIN', 'LAB_STAFF') AND organization_id IS NOT NULL)
        );
    END IF;
END $$;

-- 3. Composite index on patients for tenant-isolated pagination and ordering
CREATE INDEX IF NOT EXISTS idx_patient_org_created
ON patients (organization_id, created_at DESC);

-- 4. Composite index on patients for tenant-scoped patient code lookups
CREATE INDEX IF NOT EXISTS idx_patient_org_code
ON patients (organization_id, patient_code);

-- 5. Index on refresh_tokens token_hash for rapid O(1) hash validation
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash
ON refresh_tokens (token_hash);
