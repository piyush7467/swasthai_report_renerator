-- =========================================================================
-- SwasthAI Report Generator - Expand Test Parameter Calculation Types (V24)
-- =========================================================================

-- 1. Drop existing calculation_type check constraints on test_parameters and test_parameter_results
ALTER TABLE test_parameters
    DROP CONSTRAINT IF EXISTS test_parameters_calculation_type_check;

ALTER TABLE test_parameters
    DROP CONSTRAINT IF EXISTS chk_test_parameter_calculation_type;

ALTER TABLE test_parameter_results
    DROP CONSTRAINT IF EXISTS test_parameter_results_calculation_type_check;

ALTER TABLE test_parameter_results
    DROP CONSTRAINT IF EXISTS chk_tpr_calculation_type;

ALTER TABLE test_parameter_results
    DROP CONSTRAINT IF EXISTS chk_tpr_calculation_consistency;

-- 2. Add expanded calculation_type constraint to test_parameters
ALTER TABLE test_parameters
    ADD CONSTRAINT test_parameters_calculation_type_check
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

-- 3. Add expanded calculation_type constraint to test_parameter_results
ALTER TABLE test_parameter_results
    ADD CONSTRAINT test_parameter_results_calculation_type_check
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

-- 4. Re-create chk_tpr_calculation_consistency with all supported clinical calculation types
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

-- 5. Fix legacy/misconfigured data in test_parameters and test_parameter_results
UPDATE test_parameters
SET calculation_type = 'MCH'
WHERE (code = 'MCH' OR code ILIKE '%MCH%')
  AND code NOT ILIKE '%MCHC%'
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'MCHC'
WHERE (code = 'MCHC' OR code ILIKE '%MCHC%')
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'VLDL'
WHERE (code = 'VLDL' OR code ILIKE '%VLDL%')
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'LDL_FRIEDEWALD'
WHERE (code IN ('LDL', 'LDL_C', 'LDL_FRIEDEWALD'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'NON_HDL_CHOLESTEROL'
WHERE (code IN ('NON_HDL', 'NON_HDL_C', 'NON_HDL_CHOLESTEROL'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'GLOBULIN'
WHERE (code IN ('GLOB', 'GLOBULIN', 'SERUM_GLOBULIN'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'INDIRECT_BILIRUBIN'
WHERE (code IN ('IBIL', 'INDIRECT_BILIRUBIN', 'BILIRUBIN_INDIRECT'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'AG_RATIO'
WHERE (code IN ('AG_RATIO', 'A_G_RATIO', 'ALB_GLOB_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'BUN_CREATININE_RATIO'
WHERE (code IN ('BUN_CREATININE_RATIO', 'BUN_CREAT_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'UREA_CREATININE_RATIO'
WHERE (code IN ('UREA_CREATININE_RATIO', 'UREA_CREAT_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'EGFR_CKD_EPI_2021'
WHERE (code IN ('EGFR', 'E_GFR', 'EGFR_CKD_EPI', 'EGFR_CKD_EPI_2021'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'ANION_GAP'
WHERE (code IN ('ANION_GAP', 'AGAP'))
  AND input_type = 'CALCULATED';

UPDATE test_parameters
SET calculation_type = 'ANION_GAP_K'
WHERE (code IN ('ANION_GAP_K', 'AGAP_K'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'MCH'
WHERE (parameter_code = 'MCH' OR parameter_code ILIKE '%MCH%')
  AND parameter_code NOT ILIKE '%MCHC%'
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'MCHC'
WHERE (parameter_code = 'MCHC' OR parameter_code ILIKE '%MCHC%')
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'VLDL'
WHERE (parameter_code = 'VLDL' OR parameter_code ILIKE '%VLDL%')
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'LDL_FRIEDEWALD'
WHERE (parameter_code IN ('LDL', 'LDL_C', 'LDL_FRIEDEWALD'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'NON_HDL_CHOLESTEROL'
WHERE (parameter_code IN ('NON_HDL', 'NON_HDL_C', 'NON_HDL_CHOLESTEROL'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'GLOBULIN'
WHERE (parameter_code IN ('GLOB', 'GLOBULIN', 'SERUM_GLOBULIN'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'INDIRECT_BILIRUBIN'
WHERE (parameter_code IN ('IBIL', 'INDIRECT_BILIRUBIN', 'BILIRUBIN_INDIRECT'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'AG_RATIO'
WHERE (parameter_code IN ('AG_RATIO', 'A_G_RATIO', 'ALB_GLOB_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'BUN_CREATININE_RATIO'
WHERE (parameter_code IN ('BUN_CREATININE_RATIO', 'BUN_CREAT_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'UREA_CREATININE_RATIO'
WHERE (parameter_code IN ('UREA_CREATININE_RATIO', 'UREA_CREAT_RATIO'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'EGFR_CKD_EPI_2021'
WHERE (parameter_code IN ('EGFR', 'E_GFR', 'EGFR_CKD_EPI', 'EGFR_CKD_EPI_2021'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'ANION_GAP'
WHERE (parameter_code IN ('ANION_GAP', 'AGAP'))
  AND input_type = 'CALCULATED';

UPDATE test_parameter_results
SET calculation_type = 'ANION_GAP_K'
WHERE (parameter_code IN ('ANION_GAP_K', 'AGAP_K'))
  AND input_type = 'CALCULATED';
