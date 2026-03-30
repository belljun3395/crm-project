ALTER TABLE journeys
    ADD COLUMN trigger_segment_event VARCHAR(32) NULL,
    ADD COLUMN trigger_segment_watch_fields TEXT NULL,
    ADD COLUMN trigger_segment_count_threshold BIGINT NULL;

CREATE TABLE journey_segment_user_states
(
    id                 BIGSERIAL PRIMARY KEY,
    journey_id         BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    in_segment         BOOLEAN      NOT NULL DEFAULT FALSE,
    attributes_hash    VARCHAR(128) NULL,
    transition_version BIGINT       NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_segment_user_states_journey_id
        FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT uq_journey_segment_user_states_journey_user UNIQUE (journey_id, user_id)
);

CREATE INDEX idx_journey_segment_user_states_journey_id ON journey_segment_user_states (journey_id);
CREATE INDEX idx_journey_segment_user_states_journey_in_segment ON journey_segment_user_states (journey_id, in_segment);

CREATE TABLE journey_segment_count_states
(
    id                 BIGSERIAL PRIMARY KEY,
    journey_id         BIGINT      NOT NULL,
    last_count         BIGINT      NOT NULL DEFAULT 0,
    transition_version BIGINT      NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_journey_segment_count_states_journey_id
        FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT uq_journey_segment_count_states_journey_id UNIQUE (journey_id)
);

CREATE INDEX idx_journey_segment_count_states_updated_at ON journey_segment_count_states (updated_at);
