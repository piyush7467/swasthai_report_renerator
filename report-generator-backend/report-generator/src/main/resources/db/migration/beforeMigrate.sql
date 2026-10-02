-- =========================================================================
-- Flyway Callback: Clean up any previous failed migrations in PostgreSQL
-- Allows automatic self-healing on restart without manual intervention
-- =========================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
    ) THEN
        DELETE FROM flyway_schema_history WHERE success = false;
    END IF;
END $$;
