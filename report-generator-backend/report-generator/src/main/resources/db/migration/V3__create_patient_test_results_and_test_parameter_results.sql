-- =========================================================================
-- SwasthAI Report Generator
-- V3 - Patient Test Results & Test Parameter Results
-- =========================================================================

CREATE TABLE IF NOT EXISTS patient_test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ref_id VARCHAR(30) NOT NULL,

    organization_id UUID NOT NULL,
    test_id UUID NOT NULL,

    patient_ref_id VARCHAR(50) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    result_version INTEGER NOT NULL DEFAULT 1,

    performed_at TIMESTAMPTZ,
    finalized_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_patient_test_result_ref_id
        UNIQUE (ref_id),

    CONSTRAINT fk_patient_test_result_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_test_result_test
        FOREIGN KEY (test_id)
        REFERENCES tests (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ptr_status
        CHECK (status IN ('DRAFT', 'CALCULATED', 'FINALIZED')),

    CONSTRAINT chk_ptr_result_version
        CHECK (result_version >= 1)
);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_organization
    ON patient_test_results (organization_id);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_test
    ON patient_test_results (test_id);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_patient
    ON patient_test_results (patient_ref_id);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_status
    ON patient_test_results (status);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_performed_at
    ON patient_test_results (performed_at);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_ref_id
    ON patient_test_results (ref_id);

CREATE INDEX IF NOT EXISTS idx_patient_test_result_org_created
    ON patient_test_results (organization_id, created_at DESC);


-- =========================================================================
-- Test Parameter Results
-- =========================================================================

CREATE TABLE IF NOT EXISTS test_parameter_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ref_id VARCHAR(30) NOT NULL,

    patient_test_result_id UUID NOT NULL,
    test_parameter_id UUID NOT NULL,

    value VARCHAR(500),
    numeric_value NUMERIC(19, 6),

    flag VARCHAR(20),

    -- Snapshot of parameter metadata
    parameter_code VARCHAR(50) NOT NULL,
    parameter_name VARCHAR(150) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    calculation_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    calculation_version VARCHAR(50),

    unit VARCHAR(50),

    reference_min NUMERIC(19, 6),
    reference_max NUMERIC(19, 6),

    critical_low NUMERIC(19, 6),
    critical_high NUMERIC(19, 6),

    display_order INTEGER NOT NULL DEFAULT 1,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_test_parameter_result_ref_id
        UNIQUE (ref_id),

    CONSTRAINT uk_ptr_test_parameter
        UNIQUE (patient_test_result_id, test_parameter_id),

    CONSTRAINT fk_test_parameter_result_ptr
        FOREIGN KEY (patient_test_result_id)
        REFERENCES patient_test_results (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_test_parameter_result_param
        FOREIGN KEY (test_parameter_id)
        REFERENCES test_parameters (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_tpr_flag
        CHECK (
            flag IS NULL
            OR flag IN (
                'NORMAL',
                'LOW',
                'HIGH',
                'CRITICAL_LOW',
                'CRITICAL_HIGH'
            )
        ),

    CONSTRAINT chk_tpr_data_type
        CHECK (
            data_type IN (
                'INTEGER',
                'DECIMAL',
                'TEXT',
                'BOOLEAN',
                'DATE',
                'DATETIME',
                'ENUM'
            )
        ),

    CONSTRAINT chk_tpr_input_type
        CHECK (
            input_type IN (
                'MANUAL',
                'CALCULATED'
            )
        ),

    CONSTRAINT chk_tpr_calculation_type
        CHECK (
            calculation_type IN (
                'NONE',
                'MCV',
                'MCH',
                'MCHC'
            )
        ),

    CONSTRAINT chk_tpr_calculation_consistency
        CHECK (
            (
                input_type = 'MANUAL'
                AND calculation_type = 'NONE'
            )
            OR
            (
                input_type = 'CALCULATED'
                AND calculation_type IN ('MCV', 'MCH', 'MCHC')
            )
        ),

    CONSTRAINT chk_tpr_display_order
        CHECK (display_order >= 1)
);


CREATE INDEX IF NOT EXISTS idx_tpr_patient_test_result
    ON test_parameter_results (patient_test_result_id);

CREATE INDEX IF NOT EXISTS idx_tpr_test_parameter
    ON test_parameter_results (test_parameter_id);

CREATE INDEX IF NOT EXISTS idx_tpr_ref_id
    ON test_parameter_results (ref_id);

CREATE INDEX IF NOT EXISTS idx_tpr_flag
    ON test_parameter_results (flag);

CREATE INDEX IF NOT EXISTS idx_tpr_parameter_code
    ON test_parameter_results (parameter_code);

CREATE INDEX IF NOT EXISTS idx_tpr_display_order
    ON test_parameter_results (
        patient_test_result_id,
        display_order
    );