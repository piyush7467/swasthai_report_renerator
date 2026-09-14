-- =========================================================================
-- SwasthAI Report Generator
-- V11 - Add Historical Report Snapshots for Immutability and PDF Integrity
-- =========================================================================

-- 1. Add historical snapshot columns to reports table
ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS patient_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS patient_salutation VARCHAR(20),
    ADD COLUMN IF NOT EXISTS patient_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS patient_gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS patient_date_of_birth_known BOOLEAN,
    ADD COLUMN IF NOT EXISTS patient_date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS patient_age_at_reporting_value INTEGER,
    ADD COLUMN IF NOT EXISTS patient_age_at_reporting_unit VARCHAR(10),
    ADD COLUMN IF NOT EXISTS patient_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS patient_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS patient_address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS patient_weight_kg NUMERIC(6, 3),

    ADD COLUMN IF NOT EXISTS organization_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS organization_code VARCHAR(50),

    ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS created_by_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS finalized_by_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS finalized_by_email VARCHAR(255);

-- 2. Add historical snapshot columns to report_test_results table
ALTER TABLE report_test_results
    ADD COLUMN IF NOT EXISTS test_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS test_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS test_short_name VARCHAR(75),
    ADD COLUMN IF NOT EXISTS sample_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS custom_sample_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS specimen_container VARCHAR(150),
    ADD COLUMN IF NOT EXISTS report_section VARCHAR(100);

-- 3. Safe backfill for existing finalized/draft reports (if any exist in the database)
UPDATE reports r
SET
    patient_name = p.name,
    patient_salutation = p.salutation,
    patient_code = p.patient_code,
    patient_gender = p.gender,
    patient_date_of_birth_known = p.date_of_birth_known,
    patient_date_of_birth = p.date_of_birth,
    patient_age_at_reporting_value = CASE
        WHEN p.date_of_birth_known = TRUE AND p.date_of_birth IS NOT NULL
        THEN EXTRACT(YEAR FROM AGE(COALESCE(r.finalized_at, r.created_at), p.date_of_birth))::INTEGER
        ELSE p.age_value
    END,
    patient_age_at_reporting_unit = CASE
        WHEN p.date_of_birth_known = TRUE AND p.date_of_birth IS NOT NULL
        THEN 'YEARS'
        ELSE p.age_unit
    END,
    patient_phone = p.phone,
    patient_email = p.email,
    patient_address = p.address,
    patient_weight_kg = p.weight_kg
FROM patients p
WHERE r.patient_ref_id = p.ref_id
  AND r.patient_name IS NULL;

UPDATE reports r
SET
    organization_name = o.name,
    organization_code = o.code
FROM organizations o
WHERE r.organization_id = o.id
  AND r.organization_name IS NULL;

UPDATE reports r
SET
    created_by_name = u.name,
    created_by_email = u.email
FROM users u
WHERE r.created_by = u.id
  AND r.created_by_name IS NULL;

UPDATE reports r
SET
    finalized_by_name = u.name,
    finalized_by_email = u.email
FROM users u
WHERE r.finalized_by IS NOT NULL
  AND r.finalized_by = u.id
  AND r.finalized_by_name IS NULL;

UPDATE report_test_results rtr
SET
    test_code = t.code,
    test_name = t.name,
    test_short_name = t.short_name,
    sample_type = t.sample_type,
    custom_sample_type = t.custom_sample_type,
    specimen_container = t.specimen_container,
    report_section = t.report_section
FROM tests t
WHERE rtr.test_id = t.id
  AND rtr.test_code IS NULL;