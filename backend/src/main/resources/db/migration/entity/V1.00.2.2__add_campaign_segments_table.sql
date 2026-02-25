CREATE TABLE campaign_segments
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT      NOT NULL,
    segment_id  BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_campaign_segments_campaign_segment (campaign_id, segment_id),
    INDEX idx_campaign_segments_segment_id (segment_id),
    INDEX idx_campaign_segments_campaign_id (campaign_id),

    CONSTRAINT fk_campaign_segments_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_campaign_segments_segment
        FOREIGN KEY (segment_id) REFERENCES segments (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
