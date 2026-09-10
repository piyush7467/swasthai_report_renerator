ALTER TABLE reports
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by UUID;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id)
        ON DELETE RESTRICT;

CREATE INDEX idx_reports_deleted_at
    ON reports(deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE INDEX idx_reports_org_deleted_created
    ON reports(organization_id, deleted_at, created_at DESC);

CREATE INDEX idx_reports_deleted_by
    ON reports(deleted_by);