-- Migration V21: Create plan_upgrade_requests table for manual plan upgrade workflow

CREATE TABLE plan_upgrade_requests
(
    id UUID PRIMARY KEY,
    ref_id VARCHAR(40) NOT NULL,

    organization_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    requested_by_email VARCHAR(255) NOT NULL,

    current_plan_id UUID NOT NULL,
    current_plan_name VARCHAR(100) NOT NULL,

    requested_plan_id UUID NOT NULL,
    requested_plan_name VARCHAR(100) NOT NULL,

    current_active_staff_count INT NOT NULL DEFAULT 0,
    requested_staff_capacity INT NOT NULL DEFAULT 3,

    reason VARCHAR(500),
    contact_name VARCHAR(150) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    additional_message VARCHAR(1000),

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    admin_notes VARCHAR(1000),
    rejection_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_plan_upgrade_requests_ref_id
        UNIQUE (ref_id),

    CONSTRAINT fk_plan_upgrade_requests_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_plan_upgrade_requests_requested_by
        FOREIGN KEY (requested_by_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_plan_upgrade_requests_current_plan
        FOREIGN KEY (current_plan_id)
        REFERENCES plans(id),

    CONSTRAINT fk_plan_upgrade_requests_requested_plan
        FOREIGN KEY (requested_plan_id)
        REFERENCES plans(id),

    CONSTRAINT fk_plan_upgrade_requests_reviewed_by
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_plan_upgrade_requests_status
        CHECK (status IN ('PENDING', 'CONTACTED', 'APPROVED', 'REJECTED', 'CANCELLED')),

    CONSTRAINT chk_plan_upgrade_requests_counts
        CHECK (current_active_staff_count >= 0 AND requested_staff_capacity >= 1)
);

CREATE INDEX idx_plan_upgrade_requests_org_status
    ON plan_upgrade_requests(organization_id, status);

CREATE INDEX idx_plan_upgrade_requests_status
    ON plan_upgrade_requests(status);

CREATE INDEX idx_plan_upgrade_requests_created_at
    ON plan_upgrade_requests(created_at);
