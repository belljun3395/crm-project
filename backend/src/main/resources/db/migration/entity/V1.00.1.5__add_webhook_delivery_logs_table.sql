CREATE TABLE webhook_delivery_logs
(
    id              BIGSERIAL PRIMARY KEY,
    webhook_id      BIGINT        NOT NULL,
    event_id        VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(255)  NOT NULL,
    delivery_status VARCHAR(50)   NOT NULL,
    attempt_count   INT           NOT NULL DEFAULT 1,
    response_status INT           NULL,
    error_message   VARCHAR(1024) NULL,
    delivered_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_webhook_delivery_logs_webhook
        FOREIGN KEY (webhook_id) REFERENCES webhooks (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_webhook_delivery_logs_webhook_id_delivered_at ON webhook_delivery_logs (webhook_id, delivered_at);
CREATE INDEX idx_webhook_delivery_logs_status_delivered_at ON webhook_delivery_logs (delivery_status, delivered_at);
