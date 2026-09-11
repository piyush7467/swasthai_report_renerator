CREATE TABLE plans
(
    id UUID PRIMARY KEY,

    ref_id VARCHAR(40) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    annual_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_plans_ref_id
        UNIQUE (ref_id),

    CONSTRAINT uk_plans_code
        UNIQUE (code),

    CONSTRAINT chk_plans_annual_price
        CHECK (annual_price >= 0),

    CONSTRAINT chk_plans_currency
        CHECK (length(currency) = 3)
);


CREATE TABLE licenses
(
    id UUID PRIMARY KEY,

    ref_id VARCHAR(40) NOT NULL,

    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL,

    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    payment_reference VARCHAR(150),

    payment_verified_by UUID,
    payment_verified_at TIMESTAMPTZ,

    lock_version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_licenses_ref_id
        UNIQUE (ref_id),

    CONSTRAINT uk_licenses_organization
        UNIQUE (organization_id),

    CONSTRAINT fk_licenses_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_licenses_plan
        FOREIGN KEY (plan_id)
        REFERENCES plans(id),

    CONSTRAINT fk_licenses_payment_verified_by
        FOREIGN KEY (payment_verified_by)
        REFERENCES users(id),

    CONSTRAINT chk_licenses_status
        CHECK (status IN ('ACTIVE', 'EXPIRED')),

    CONSTRAINT chk_licenses_dates
        CHECK (expires_at > started_at),

    CONSTRAINT chk_licenses_lock_version
        CHECK (lock_version >= 0)
);


CREATE INDEX idx_plans_active
    ON plans(active);


CREATE INDEX idx_licenses_org_status
    ON licenses(organization_id, status);


CREATE INDEX idx_licenses_expires_at
    ON licenses(expires_at);