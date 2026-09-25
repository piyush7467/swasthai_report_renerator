-- Migration V22: Add report quotas to plans table

ALTER TABLE plans
    ADD COLUMN max_reports_per_month INT NOT NULL DEFAULT 100,
    ADD COLUMN max_reports_per_day INT NOT NULL DEFAULT 25;

ALTER TABLE plans
    ADD CONSTRAINT chk_plans_max_reports_per_month
    CHECK (max_reports_per_month >= 0);

ALTER TABLE plans
    ADD CONSTRAINT chk_plans_max_reports_per_day
    CHECK (max_reports_per_day >= 0);

-- Update default limits for seeded plans
UPDATE plans SET max_reports_per_month = 100, max_reports_per_day = 25 WHERE code = 'STARTER';
UPDATE plans SET max_reports_per_month = 1000, max_reports_per_day = 150 WHERE code = 'PROFESSIONAL';
UPDATE plans SET max_reports_per_month = 10000, max_reports_per_day = 1000 WHERE code = 'ENTERPRISE';
