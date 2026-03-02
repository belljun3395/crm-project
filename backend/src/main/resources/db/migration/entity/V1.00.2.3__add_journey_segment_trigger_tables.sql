ALTER TABLE journeys
    ADD COLUMN trigger_segment_event VARCHAR(32) NULL AFTER trigger_segment_id,
    ADD COLUMN trigger_segment_watch_fields LONGTEXT NULL AFTER trigger_segment_event,
    ADD COLUMN trigger_segment_count_threshold BIGINT NULL AFTER trigger_segment_watch_fields;

CREATE TABLE journey_segment_user_states
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_id         BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    in_segment         BOOLEAN      NOT NULL DEFAULT FALSE,
    attributes_hash    VARCHAR(128) NULL,
    transition_version BIGINT       NOT NULL DEFAULT 0,
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_segment_user_states_journey_id
        FOREIGN KEY (journey_id) REFERENCES journeys (id),
    UNIQUE KEY uq_journey_segment_user_states_journey_user (journey_id, user_id),
    INDEX idx_journey_segment_user_states_journey_id (journey_id),
    INDEX idx_journey_segment_user_states_journey_in_segment (journey_id, in_segment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journey_segment_count_states
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    journey_id         BIGINT      NOT NULL,
    last_count         BIGINT      NOT NULL DEFAULT 0,
    transition_version BIGINT      NOT NULL DEFAULT 0,
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_segment_count_states_journey_id
        FOREIGN KEY (journey_id) REFERENCES journeys (id),
    UNIQUE KEY uq_journey_segment_count_states_journey_id (journey_id),
    INDEX idx_journey_segment_count_states_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
