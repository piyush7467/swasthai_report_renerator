-- =========================================================================
-- SwasthAI Report Generator
-- V4 - Reports, Report Test Results & Test Parameter Results Adaptation
-- =========================================================================

CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ref_id VARCHAR(30) NOT NULL,

    organization_id UUID NOT NULL,
    patient_ref_id VARCHAR(50) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    report_version INTEGER NOT NULL DEFAULT 1,
    lock_version BIGINT NOT NULL DEFAULT 0,

    created_by UUID NOT NULL,
    finalized_by UUID,

    finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_reports_ref_id
        UNIQUE (ref_id),

    CONSTRAINT fk_reports_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reports_created_by
        FOREIGN KEY (created_by)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reports_finalized_by
        FOREIGN KEY (finalized_by)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_reports_status
        CHECK (status IN ('DRAFT', 'CALCULATED', 'FINALIZED')),

    CONSTRAINT chk_reports_report_version
        CHECK (report_version >= 1),

    CONSTRAINT chk_reports_lock_version
        CHECK (lock_version >= 0)
);

CREATE INDEX IF NOT EXISTS idx_reports_organization
    ON reports (organization_id);

CREATE INDEX IF NOT EXISTS idx_reports_patient
    ON reports (patient_ref_id);

CREATE INDEX IF NOT EXISTS idx_reports_status
    ON reports (status);

CREATE INDEX IF NOT EXISTS idx_reports_created_by
    ON reports (created_by);

CREATE INDEX IF NOT EXISTS idx_reports_finalized_by
    ON reports (finalized_by);

CREATE INDEX IF NOT EXISTS idx_reports_created_at
    ON reports (created_at);

CREATE INDEX IF NOT EXISTS idx_reports_ref_id
    ON reports (ref_id);

CREATE INDEX IF NOT EXISTS idx_reports_org_created
    ON reports (organization_id, created_at DESC);


-- =========================================================================
-- Report Test Results
-- =========================================================================

CREATE TABLE IF NOT EXISTS report_test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ref_id VARCHAR(30) NOT NULL,

    report_id UUID NOT NULL,
    test_id UUID NOT NULL,

    display_order INTEGER NOT NULL DEFAULT 1,
    test_version INTEGER NOT NULL DEFAULT 1,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_report_test_results_ref_id
        UNIQUE (ref_id),

    CONSTRAINT uk_report_test_results_report_test
        UNIQUE (report_id, test_id),

    CONSTRAINT fk_report_test_results_report
        FOREIGN KEY (report_id)
        REFERENCES reports (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_report_test_results_test
        FOREIGN KEY (test_id)
        REFERENCES tests (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_report_test_results_display_order
        CHECK (display_order >= 1),

    CONSTRAINT chk_report_test_results_test_version
        CHECK (test_version >= 1)
);

CREATE INDEX IF NOT EXISTS idx_report_test_results_report
    ON report_test_results (report_id);

CREATE INDEX IF NOT EXISTS idx_report_test_results_test
    ON report_test_results (test_id);

CREATE INDEX IF NOT EXISTS idx_report_test_results_ref_id
    ON report_test_results (ref_id);

CREATE INDEX IF NOT EXISTS idx_report_test_results_order
    ON report_test_results (report_id, display_order);


-- =========================================================================
-- Adapt test_parameter_results for Report Workflow
-- =========================================================================

-- Make patient_test_result_id nullable for backward compatibility with V3
ALTER TABLE test_parameter_results
    ALTER COLUMN patient_test_result_id DROP NOT NULL;

-- Add report_test_result_id column
ALTER TABLE test_parameter_results
    ADD COLUMN IF NOT EXISTS report_test_result_id UUID;

-- Add foreign key from test_parameter_results to report_test_results
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_test_parameter_result_rtr'
    ) THEN
        ALTER TABLE test_parameter_results
            ADD CONSTRAINT fk_test_parameter_result_rtr
            FOREIGN KEY (report_test_result_id)
            REFERENCES report_test_results (id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- Add unique constraint for report test result parameter
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_rtr_test_parameter'
    ) THEN
        ALTER TABLE test_parameter_results
            ADD CONSTRAINT uk_rtr_test_parameter
            UNIQUE (report_test_result_id, test_parameter_id);
    END IF;
END $$;

-- Ensure at least one parent is present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_tpr_parent'
    ) THEN
        ALTER TABLE test_parameter_results
            ADD CONSTRAINT chk_tpr_parent
            CHECK (patient_test_result_id IS NOT NULL OR report_test_result_id IS NOT NULL);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tpr_report_test_result
    ON test_parameter_results (report_test_result_id);

CREATE INDEX IF NOT EXISTS idx_tpr_rtr_display_order
    ON test_parameter_results (report_test_result_id, display_order);