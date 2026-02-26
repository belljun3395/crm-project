CREATE TABLE webhook_delivery_dead_letters
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id      BIGINT         NOT NULL,
    event_id        VARCHAR(255)   NOT NULL,
    event_type      VARCHAR(255)   NOT NULL,
    payload_json    LONGTEXT       NOT NULL,
    delivery_status VARCHAR(50)    NOT NULL COMMENT 'FAILED, BLOCKED',
    attempt_count   INT            NOT NULL DEFAULT 1,
    response_status INT            NULL,
    error_message   VARCHAR(1024)  NULL,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_webhook_dead_letters_webhook_id_created_at (webhook_id, created_at),
    INDEX idx_webhook_dead_letters_status_created_at (delivery_status, created_at),

    CONSTRAINT fk_webhook_dead_letters_webhook
        FOREIGN KEY (webhook_id) REFERENCES webhooks (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
