CREATE TABLE action_dispatch_histories
(
    id                   BIGSERIAL PRIMARY KEY,
    channel              VARCHAR(32)   NOT NULL,
    status               VARCHAR(32)   NOT NULL,
    destination          VARCHAR(1024) NOT NULL,
    subject              VARCHAR(255)  NULL,
    body                 TEXT          NOT NULL,
    variables_json       TEXT          NULL,
    provider_message_id  VARCHAR(255)  NULL,
    error_code           VARCHAR(128)  NULL,
    error_message        VARCHAR(2048) NULL,
    campaign_id          BIGINT        NULL,
    journey_execution_id BIGINT        NULL,
    created_at           TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_action_dispatch_histories_created_at ON action_dispatch_histories (created_at);
CREATE INDEX idx_action_dispatch_histories_campaign_id_created_at ON action_dispatch_histories (campaign_id, created_at);
CREATE INDEX idx_action_dispatch_histories_journey_execution_id_created_at ON action_dispatch_histories (journey_execution_id, created_at);
