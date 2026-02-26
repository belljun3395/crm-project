CREATE TABLE action_dispatch_histories
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel              VARCHAR(32)   NOT NULL,
    status               VARCHAR(32)   NOT NULL,
    destination          VARCHAR(1024) NOT NULL,
    subject              VARCHAR(255)  NULL,
    body                 LONGTEXT      NOT NULL,
    variables_json       LONGTEXT      NULL,
    provider_message_id  VARCHAR(255)  NULL,
    error_code           VARCHAR(128)  NULL,
    error_message        VARCHAR(2048) NULL,
    campaign_id          BIGINT        NULL,
    journey_execution_id BIGINT        NULL,
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_action_dispatch_histories_created_at (created_at),
    INDEX idx_action_dispatch_histories_campaign_id_created_at (campaign_id, created_at),
    INDEX idx_action_dispatch_histories_journey_execution_id_created_at (journey_execution_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
