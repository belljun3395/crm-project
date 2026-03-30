CREATE TABLE segments
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1024) NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT uq_segments_name UNIQUE (name)
);

CREATE INDEX idx_segments_created_at ON segments (created_at);

CREATE TABLE segment_conditions
(
    id              BIGSERIAL PRIMARY KEY,
    segment_id      BIGINT        NOT NULL,
    field_name      VARCHAR(255)  NOT NULL,
    operator        VARCHAR(50)   NOT NULL,
    value_type      VARCHAR(50)   NOT NULL,
    condition_value TEXT          NOT NULL,
    position        INT           NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_segment_conditions_segment
        FOREIGN KEY (segment_id) REFERENCES segments (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_segment_conditions_segment_id_position ON segment_conditions (segment_id, position);
