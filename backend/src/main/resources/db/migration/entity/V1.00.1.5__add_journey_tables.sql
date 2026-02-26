CREATE TABLE journeys
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    trigger_type       VARCHAR(32)  NOT NULL,
    trigger_event_name VARCHAR(255) NULL,
    trigger_segment_id BIGINT       NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_journeys_trigger_event_name_active (trigger_event_name, active),
    INDEX idx_journeys_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journey_steps
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_id           BIGINT        NOT NULL,
    step_order           INT           NOT NULL,
    step_type            VARCHAR(32)   NOT NULL,
    channel              VARCHAR(32)   NULL,
    destination          VARCHAR(1024) NULL,
    subject              VARCHAR(255)  NULL,
    body                 LONGTEXT      NULL,
    variables_json       LONGTEXT      NULL,
    delay_millis         BIGINT        NULL,
    condition_expression VARCHAR(1024) NULL,
    retry_count          INT           NOT NULL DEFAULT 0,
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_steps_journey_id FOREIGN KEY (journey_id) REFERENCES journeys (id),
    UNIQUE KEY uq_journey_steps_journey_order (journey_id, step_order),
    INDEX idx_journey_steps_journey_id (journey_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journey_executions
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_id         BIGINT        NOT NULL,
    event_id           BIGINT        NOT NULL,
    user_id            BIGINT        NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    current_step_order INT           NOT NULL DEFAULT 0,
    last_error         VARCHAR(2048) NULL,
    trigger_key        VARCHAR(255)  NOT NULL,
    started_at         DATETIME(6)   NOT NULL,
    completed_at       DATETIME(6)   NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_executions_journey_id FOREIGN KEY (journey_id) REFERENCES journeys (id),
    UNIQUE KEY uq_journey_executions_trigger_key (trigger_key),
    INDEX idx_journey_executions_journey_id_created_at (journey_id, created_at),
    INDEX idx_journey_executions_event_user (event_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journey_execution_histories
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_execution_id BIGINT        NOT NULL,
    journey_step_id      BIGINT        NOT NULL,
    status               VARCHAR(32)   NOT NULL,
    attempt              INT           NOT NULL DEFAULT 0,
    message              VARCHAR(2048) NULL,
    idempotency_key      VARCHAR(255)  NULL,
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_execution_histories_execution_id
        FOREIGN KEY (journey_execution_id) REFERENCES journey_executions (id),
    CONSTRAINT fk_journey_execution_histories_step_id
        FOREIGN KEY (journey_step_id) REFERENCES journey_steps (id),
    INDEX idx_journey_execution_histories_execution_id_created_at (journey_execution_id, created_at),
    INDEX idx_journey_execution_histories_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journey_step_deduplications
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_journey_step_deduplications_key (idempotency_key),
    INDEX idx_journey_step_deduplications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
