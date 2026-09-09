-- =========================================================================
-- SwasthAI Report Generator - Test Parameter Calculation Configuration Migration (V2)
-- =========================================================================

-- 1. Add input_type column defaulting to 'MANUAL'
ALTER TABLE test_parameters
ADD COLUMN IF NOT EXISTS input_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- 2. Add calculation_type column defaulting to 'NONE'
ALTER TABLE test_parameters
ADD COLUMN IF NOT EXISTS calculation_type VARCHAR(50) NOT NULL DEFAULT 'NONE';

-- 3. Ensure all existing records are set to MANUAL and NONE
UPDATE test_parameters
SET input_type = 'MANUAL'
WHERE input_type IS NULL;

UPDATE test_parameters
SET calculation_type = 'NONE'
WHERE calculation_type IS NULL;
