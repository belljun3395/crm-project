CREATE TABLE segments
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1024) NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_segments_name (name),
    INDEX idx_segments_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE segment_conditions
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id      BIGINT        NOT NULL,
    field_name      VARCHAR(255)  NOT NULL,
    operator        VARCHAR(50)   NOT NULL,
    value_type      VARCHAR(50)   NOT NULL,
    condition_value LONGTEXT      NOT NULL,
    position        INT           NOT NULL DEFAULT 1,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_segment_conditions_segment_id_position (segment_id, position),

    CONSTRAINT fk_segment_conditions_segment
        FOREIGN KEY (segment_id) REFERENCES segments (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
