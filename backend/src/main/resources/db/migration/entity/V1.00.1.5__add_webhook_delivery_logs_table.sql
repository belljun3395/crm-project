CREATE TABLE webhook_delivery_logs
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id      BIGINT        NOT NULL,
    event_id        VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(255)  NOT NULL,
    delivery_status VARCHAR(50)   NOT NULL COMMENT 'SUCCESS, FAILED, BLOCKED',
    attempt_count   INT           NOT NULL DEFAULT 1,
    response_status INT           NULL,
    error_message   VARCHAR(1024) NULL,
    delivered_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_webhook_delivery_logs_webhook_id_delivered_at (webhook_id, delivered_at),
    INDEX idx_webhook_delivery_logs_status_delivered_at (delivery_status, delivered_at),

    CONSTRAINT fk_webhook_delivery_logs_webhook
        FOREIGN KEY (webhook_id) REFERENCES webhooks (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
