-- Migration V19: Add max_lab_staff to plans table

ALTER TABLE plans
    ADD COLUMN max_lab_staff INT NOT NULL DEFAULT 3;

ALTER TABLE plans
    ADD CONSTRAINT chk_plans_max_lab_staff
    CHECK (max_lab_staff >= 1);

-- Update default limits for seeded plans
UPDATE plans SET max_lab_staff = 3 WHERE code = 'STARTER';
UPDATE plans SET max_lab_staff = 10 WHERE code = 'PROFESSIONAL';
UPDATE plans SET max_lab_staff = 50 WHERE code = 'ENTERPRISE';
