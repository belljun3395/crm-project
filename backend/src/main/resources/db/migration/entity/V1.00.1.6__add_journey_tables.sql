CREATE TABLE journeys
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    trigger_type       VARCHAR(32)  NOT NULL,
    trigger_event_name VARCHAR(255) NULL,
    trigger_segment_id BIGINT       NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_journeys_trigger_event_name_active ON journeys (trigger_event_name, active);
CREATE INDEX idx_journeys_created_at ON journeys (created_at);

CREATE TABLE journey_steps
(
    id                   BIGSERIAL PRIMARY KEY,
    journey_id           BIGINT        NOT NULL,
    step_order           INT           NOT NULL,
    step_type            VARCHAR(32)   NOT NULL,
    channel              VARCHAR(32)   NULL,
    destination          VARCHAR(1024) NULL,
    subject              VARCHAR(255)  NULL,
    body                 TEXT          NULL,
    variables_json       TEXT          NULL,
    delay_millis         BIGINT        NULL,
    condition_expression VARCHAR(1024) NULL,
    retry_count          INT           NOT NULL DEFAULT 0,
    created_at           TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_steps_journey_id FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT uq_journey_steps_journey_order UNIQUE (journey_id, step_order)
);

CREATE INDEX idx_journey_steps_journey_id ON journey_steps (journey_id);

CREATE TABLE journey_executions
(
    id                 BIGSERIAL PRIMARY KEY,
    journey_id         BIGINT        NOT NULL,
    event_id           BIGINT        NOT NULL,
    user_id            BIGINT        NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    current_step_order INT           NOT NULL DEFAULT 0,
    last_error         VARCHAR(2048) NULL,
    trigger_key        VARCHAR(255)  NOT NULL,
    started_at         TIMESTAMP(6)  NOT NULL,
    completed_at       TIMESTAMP(6)  NULL,
    created_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_executions_journey_id FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT uq_journey_executions_trigger_key UNIQUE (trigger_key)
);

CREATE INDEX idx_journey_executions_journey_id_created_at ON journey_executions (journey_id, created_at);
CREATE INDEX idx_journey_executions_event_user ON journey_executions (event_id, user_id);

CREATE TABLE journey_execution_histories
(
    id                   BIGSERIAL PRIMARY KEY,
    journey_execution_id BIGINT        NOT NULL,
    journey_step_id      BIGINT        NOT NULL,
    status               VARCHAR(32)   NOT NULL,
    attempt              INT           NOT NULL DEFAULT 0,
    message              VARCHAR(2048) NULL,
    idempotency_key      VARCHAR(255)  NULL,
    created_at           TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_execution_histories_execution_id
        FOREIGN KEY (journey_execution_id) REFERENCES journey_executions (id),
    CONSTRAINT fk_journey_execution_histories_step_id
        FOREIGN KEY (journey_step_id) REFERENCES journey_steps (id)
);

CREATE INDEX idx_journey_execution_histories_execution_id_created_at ON journey_execution_histories (journey_execution_id, created_at);
CREATE INDEX idx_journey_execution_histories_idempotency_key ON journey_execution_histories (idempotency_key);

CREATE TABLE journey_step_deduplications
(
    id              BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT uq_journey_step_deduplications_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_journey_step_deduplications_created_at ON journey_step_deduplications (created_at);
