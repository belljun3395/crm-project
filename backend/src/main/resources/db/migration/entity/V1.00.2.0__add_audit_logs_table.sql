CREATE TABLE audit_logs
(
    id             BIGSERIAL PRIMARY KEY,
    actor_id       VARCHAR(255)  NULL,
    action         VARCHAR(120)  NOT NULL,
    resource_type  VARCHAR(120)  NOT NULL,
    resource_id    VARCHAR(255)  NULL,
    request_method VARCHAR(16)   NULL,
    request_path   VARCHAR(1024) NULL,
    status_code    INT           NULL,
    detail         VARCHAR(2048) NULL,
    created_at     TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_action_created_at ON audit_logs (action, created_at);
CREATE INDEX idx_audit_logs_resource_created_at ON audit_logs (resource_type, created_at);
CREATE INDEX idx_audit_logs_actor_created_at ON audit_logs (actor_id, created_at);
