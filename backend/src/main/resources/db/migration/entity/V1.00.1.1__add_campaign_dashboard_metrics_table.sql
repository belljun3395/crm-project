CREATE TABLE campaign_dashboard_metrics
(
    id                     BIGSERIAL PRIMARY KEY,
    campaign_id            BIGINT       NOT NULL,
    metric_type            VARCHAR(50)   NOT NULL,
    metric_value           BIGINT       NOT NULL DEFAULT 0,
    time_window_start      TIMESTAMP(6)  NOT NULL,
    time_window_end        TIMESTAMP(6)  NOT NULL,
    time_window_unit       VARCHAR(20)   NOT NULL,
    created_at             TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT unique_campaign_metric_time UNIQUE (campaign_id, metric_type, time_window_start, time_window_end)
);

CREATE INDEX idx_campaign_id_metric_type ON campaign_dashboard_metrics (campaign_id, metric_type);
CREATE INDEX idx_time_window ON campaign_dashboard_metrics (time_window_start, time_window_end);
