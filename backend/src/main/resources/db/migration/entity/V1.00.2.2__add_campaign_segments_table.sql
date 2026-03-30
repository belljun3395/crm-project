CREATE TABLE campaign_segments
(
    id          BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT      NOT NULL,
    segment_id  BIGINT      NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_campaign_segments_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_campaign_segments_segment
        FOREIGN KEY (segment_id) REFERENCES segments (id)
            ON DELETE CASCADE,
    CONSTRAINT uq_campaign_segments_campaign_segment UNIQUE (campaign_id, segment_id)
);

CREATE INDEX idx_campaign_segments_segment_id ON campaign_segments (segment_id);
CREATE INDEX idx_campaign_segments_campaign_id ON campaign_segments (campaign_id);
