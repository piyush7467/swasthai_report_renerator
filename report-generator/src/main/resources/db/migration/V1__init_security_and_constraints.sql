-- =========================================================================
-- SwasthAI Report Generator - Security & Database Invariants Migration (V1)
-- =========================================================================

-- 1. Enforce invariant: Only ONE active SUPER_ADMIN can exist in the platform
CREATE UNIQUE INDEX IF NOT EXISTS uk_single_super_admin
ON users (role)
WHERE role = 'SUPER_ADMIN';

-- 2. Enforce invariant: SUPER_ADMIN has organization_id IS NULL; ORG_ADMIN & LAB_STAFF must have organization_id
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_user_org_by_role') THEN
        ALTER TABLE users ADD CONSTRAINT chk_user_org_by_role CHECK (
            (role = 'SUPER_ADMIN' AND organization_id IS NULL) OR
            (role IN ('ORG_ADMIN', 'LAB_STAFF') AND organization_id IS NOT NULL)
        );
    END IF;
END $$;

-- 3. Composite index on patients for tenant-isolated pagination and ordering
CREATE INDEX IF NOT EXISTS idx_patient_org_created
ON patients (organization_id, created_at DESC);

-- 4. Composite index on patients for tenant-scoped patient code lookups
CREATE INDEX IF NOT EXISTS idx_patient_org_code
ON patients (organization_id, patient_code);

-- 5. Index on refresh_tokens token_hash for rapid O(1) hash validation
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash
ON refresh_tokens (token_hash);
