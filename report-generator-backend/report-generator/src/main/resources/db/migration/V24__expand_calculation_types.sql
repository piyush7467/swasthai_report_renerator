-- =========================================================================
-- SwasthAI Report Generator - Expand Test Parameter Calculation Types (V24)
-- =========================================================================

-- 1. Drop existing calculation_type check constraints on test_parameter_results
ALTER TABLE test_parameter_results
    DROP CONSTRAINT IF EXISTS chk_tpr_calculation_type;

ALTER TABLE test_parameter_results
    DROP CONSTRAINT IF EXISTS chk_tpr_calculation_consistency;

-- 2. Re-create chk_tpr_calculation_type with all supported clinical calculation types
ALTER TABLE test_parameter_results
    ADD CONSTRAINT chk_tpr_calculation_type
        CHECK (
            calculation_type IN (
                'NONE',
                'MCV',
                'MCH',
                'MCHC',
                'VLDL',
                'LDL_FRIEDEWALD',
                'NON_HDL_CHOLESTEROL',
                'CHOL_HDL_RATIO',
                'LDL_HDL_RATIO',
                'INDIRECT_BILIRUBIN',
                'GLOBULIN',
                'AG_RATIO',
                'BUN_CREATININE_RATIO',
                'UREA_CREATININE_RATIO',
                'EGFR_CKD_EPI_2021',
                'ANION_GAP',
                'ANION_GAP_K'
            )
        );

-- 3. Re-create chk_tpr_calculation_consistency with all supported clinical calculation types
ALTER TABLE test_parameter_results
    ADD CONSTRAINT chk_tpr_calculation_consistency
        CHECK (
            (
                input_type = 'MANUAL'
                AND calculation_type = 'NONE'
            )
            OR
            (
                input_type = 'CALCULATED'
                AND calculation_type IN (
                    'MCV',
                    'MCH',
                    'MCHC',
                    'VLDL',
                    'LDL_FRIEDEWALD',
                    'NON_HDL_CHOLESTEROL',
                    'CHOL_HDL_RATIO',
                    'LDL_HDL_RATIO',
                    'INDIRECT_BILIRUBIN',
                    'GLOBULIN',
                    'AG_RATIO',
                    'BUN_CREATININE_RATIO',
                    'UREA_CREATININE_RATIO',
                    'EGFR_CKD_EPI_2021',
                    'ANION_GAP',
                    'ANION_GAP_K'
                )
            )
        );
