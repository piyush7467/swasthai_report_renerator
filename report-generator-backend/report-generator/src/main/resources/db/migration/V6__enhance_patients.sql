ALTER TABLE patients
    ADD COLUMN salutation VARCHAR(20);

ALTER TABLE patients
    ADD COLUMN date_of_birth_known BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE patients
    ADD COLUMN age_value INTEGER;

ALTER TABLE patients
    ADD COLUMN age_unit VARCHAR(10);

ALTER TABLE patients
    ADD COLUMN weight_kg NUMERIC(6,3);

ALTER TABLE patients
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE patients
    ADD COLUMN deleted_by UUID;


-- Existing patients already have DOB,
-- so keep them as DOB-known patients.
UPDATE patients
SET date_of_birth_known = TRUE
WHERE date_of_birth_known IS NULL;


-- Existing patients don't have salutation because
-- this field is being introduced in V6.
-- OTHER is the safest neutral value.
UPDATE patients
SET salutation = 'OTHER'
WHERE salutation IS NULL;


ALTER TABLE patients
    ALTER COLUMN salutation SET NOT NULL;


-- Existing schema had date_of_birth NOT NULL.
-- DOB must now be nullable when DOB is unknown.
ALTER TABLE patients
    ALTER COLUMN date_of_birth DROP NOT NULL;


ALTER TABLE patients
    ADD CONSTRAINT fk_patient_deleted_by
    FOREIGN KEY (deleted_by)
    REFERENCES users(id);


ALTER TABLE patients
    ADD CONSTRAINT chk_patient_age_value_positive
    CHECK (
        age_value IS NULL
        OR age_value > 0
    );


ALTER TABLE patients
    ADD CONSTRAINT chk_patient_weight_positive
    CHECK (
        weight_kg IS NULL
        OR weight_kg > 0
    );


ALTER TABLE patients
    ADD CONSTRAINT chk_patient_age_unit
    CHECK (
        age_unit IS NULL
        OR age_unit IN (
            'DAYS',
            'WEEKS',
            'MONTHS',
            'YEARS'
        )
    );


ALTER TABLE patients
    ADD CONSTRAINT chk_patient_dob_age_consistency
    CHECK (
        (
            date_of_birth_known = TRUE
            AND date_of_birth IS NOT NULL
            AND age_value IS NULL
            AND age_unit IS NULL
        )
        OR
        (
            date_of_birth_known = FALSE
            AND date_of_birth IS NULL
            AND age_value IS NOT NULL
            AND age_unit IS NOT NULL
        )
    );


ALTER TABLE patients
    ADD CONSTRAINT chk_patient_salutation
    CHECK (
        salutation IN (
            'MR',
            'MRS',
            'MISS',
            'MASTER',
            'BABY',
            'BABY_OF',
            'MS',
            'SMT',
            'SHRI',
            'SHRIMAN',
            'SHRIMATI',
            'DR',
            'PROF',
            'OTHER'
        )
    );


CREATE INDEX idx_patient_org_deleted_at
    ON patients (organization_id, deleted_at);


CREATE INDEX idx_patient_org_email
    ON patients (organization_id, email);