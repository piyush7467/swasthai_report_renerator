-- =========================================================================
-- SwasthAI Report Generator
-- V20 - Support Staff Deactivation, 10-Day Retention & Safe Historical Foreign Keys
-- =========================================================================

-- 1. Add inactive_at and deactivated_by columns to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS inactive_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deactivated_by UUID;

-- 2. Add foreign key for deactivated_by referencing users(id) with ON DELETE SET NULL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_deactivated_by'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_deactivated_by
            FOREIGN KEY (deactivated_by)
            REFERENCES users(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Composite index on users for cleanup scheduler efficiency
CREATE INDEX IF NOT EXISTS idx_users_status_inactive_at
    ON users (status, inactive_at)
    WHERE status = 'INACTIVE' AND inactive_at IS NOT NULL;

-- 4. Adjust security_audit_logs to allow NULL actor_user_id on delete (preserving audit integrity)
ALTER TABLE security_audit_logs
    ALTER COLUMN actor_user_id DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_security_audit_logs_actor'
    ) THEN
        ALTER TABLE security_audit_logs DROP CONSTRAINT fk_security_audit_logs_actor;
    END IF;

    ALTER TABLE security_audit_logs
        ADD CONSTRAINT fk_security_audit_logs_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL;
END $$;

-- 5. Adjust reports foreign keys to ON DELETE SET NULL (preserving report history & creator snapshots)
ALTER TABLE reports
    ALTER COLUMN created_by DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reports_created_by') THEN
        ALTER TABLE reports DROP CONSTRAINT fk_reports_created_by;
    END IF;
    ALTER TABLE reports
        ADD CONSTRAINT fk_reports_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reports_finalized_by') THEN
        ALTER TABLE reports DROP CONSTRAINT fk_reports_finalized_by;
    END IF;
    ALTER TABLE reports
        ADD CONSTRAINT fk_reports_finalized_by
        FOREIGN KEY (finalized_by)
        REFERENCES users(id)
        ON DELETE SET NULL;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reports_deleted_by') THEN
        ALTER TABLE reports DROP CONSTRAINT fk_reports_deleted_by;
    END IF;
    ALTER TABLE reports
        ADD CONSTRAINT fk_reports_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id)
        ON DELETE SET NULL;
END $$;

-- 6. Adjust patients deleted_by foreign key to ON DELETE SET NULL
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_patient_deleted_by') THEN
        ALTER TABLE patients DROP CONSTRAINT fk_patient_deleted_by;
    END IF;
    ALTER TABLE patients
        ADD CONSTRAINT fk_patient_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id)
        ON DELETE SET NULL;
END $$;

-- 7. Adjust report_shares shared_by_user_id foreign key to ON DELETE SET NULL
ALTER TABLE report_shares
    ALTER COLUMN shared_by_user_id DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_report_shares_user') THEN
        ALTER TABLE report_shares DROP CONSTRAINT fk_report_shares_user;
    END IF;
    ALTER TABLE report_shares
        ADD CONSTRAINT fk_report_shares_user
        FOREIGN KEY (shared_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL;
END $$;

-- 8. Adjust licenses payment_verified_by foreign key to ON DELETE SET NULL
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_licenses_payment_verified_by') THEN
        ALTER TABLE licenses DROP CONSTRAINT fk_licenses_payment_verified_by;
    END IF;
    ALTER TABLE licenses
        ADD CONSTRAINT fk_licenses_payment_verified_by
        FOREIGN KEY (payment_verified_by)
        REFERENCES users(id)
        ON DELETE SET NULL;
END $$;
