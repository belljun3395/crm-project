CREATE TABLE webhook_delivery_dead_letters
(
    id              BIGSERIAL PRIMARY KEY,
    webhook_id      BIGINT         NOT NULL,
    event_id        VARCHAR(255)   NOT NULL,
    event_type      VARCHAR(255)   NOT NULL,
    payload_json    TEXT           NOT NULL,
    delivery_status VARCHAR(50)    NOT NULL,
    attempt_count   INT            NOT NULL DEFAULT 1,
    response_status INT            NULL,
    error_message   VARCHAR(1024)  NULL,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_webhook_dead_letters_webhook
        FOREIGN KEY (webhook_id) REFERENCES webhooks (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_webhook_dead_letters_webhook_id_created_at ON webhook_delivery_dead_letters (webhook_id, created_at);
CREATE INDEX idx_webhook_dead_letters_status_created_at ON webhook_delivery_dead_letters (delivery_status, created_at);
