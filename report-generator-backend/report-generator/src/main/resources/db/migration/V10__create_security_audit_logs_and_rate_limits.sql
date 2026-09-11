CREATE TABLE security_audit_logs
(
    id UUID PRIMARY KEY,

    ref_id VARCHAR(40) NOT NULL,

    actor_user_id UUID NOT NULL,
    actor_email VARCHAR(255) NOT NULL,

    action VARCHAR(50) NOT NULL,

    target_organization_id UUID,
    target_organization_ref_id VARCHAR(50),

    target_report_id UUID,
    target_report_ref_id VARCHAR(50),

    justification VARCHAR(500) NOT NULL,

    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),

    ip_address VARCHAR(45),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_security_audit_logs_ref_id
        UNIQUE (ref_id),

    CONSTRAINT fk_security_audit_logs_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_security_audit_logs_action
    ON security_audit_logs(action);

CREATE INDEX idx_security_audit_logs_target_org
    ON security_audit_logs(target_organization_ref_id);

CREATE INDEX idx_security_audit_logs_created_at
    ON security_audit_logs(created_at);


CREATE TABLE auth_rate_limit_attempts
(
    key_hash VARCHAR(64) PRIMARY KEY,

    attempt_count INT NOT NULL DEFAULT 1,
    window_start_epoch_second BIGINT NOT NULL,
    locked_until_epoch_second BIGINT NOT NULL DEFAULT 0,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_rate_limit_attempt_count
        CHECK (attempt_count >= 0),

    CONSTRAINT chk_rate_limit_window_start
        CHECK (window_start_epoch_second >= 0),

    CONSTRAINT chk_rate_limit_locked_until
        CHECK (locked_until_epoch_second >= 0)
);

CREATE INDEX idx_rate_limit_locked_until
    ON auth_rate_limit_attempts(locked_until_epoch_second);
