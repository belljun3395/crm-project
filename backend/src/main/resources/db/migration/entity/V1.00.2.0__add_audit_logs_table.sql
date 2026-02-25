CREATE TABLE audit_logs
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id       VARCHAR(255)  NULL,
    action         VARCHAR(120)  NOT NULL,
    resource_type  VARCHAR(120)  NOT NULL,
    resource_id    VARCHAR(255)  NULL,
    request_method VARCHAR(16)   NULL,
    request_path   VARCHAR(1024) NULL,
    status_code    INT           NULL,
    detail         VARCHAR(2048) NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_audit_logs_created_at (created_at),
    INDEX idx_audit_logs_action_created_at (action, created_at),
    INDEX idx_audit_logs_resource_created_at (resource_type, created_at),
    INDEX idx_audit_logs_actor_created_at (actor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
