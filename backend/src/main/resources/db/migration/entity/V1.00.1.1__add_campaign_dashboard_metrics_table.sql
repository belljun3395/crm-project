CREATE TABLE campaign_dashboard_metrics
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id            BIGINT       NOT NULL,
    metric_type            VARCHAR(50)  NOT NULL COMMENT 'Type of metric: EVENT_COUNT, UNIQUE_USER_COUNT, TOTAL_USER_COUNT',
    metric_value           BIGINT       NOT NULL DEFAULT 0,
    time_window_start      DATETIME(6)  NOT NULL COMMENT 'Start of the time window for this metric',
    time_window_end        DATETIME(6)  NOT NULL COMMENT 'End of the time window for this metric',
    time_window_unit       VARCHAR(20)  NOT NULL COMMENT 'MINUTE, HOUR, DAY, WEEK, MONTH',
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_campaign_id_metric_type (campaign_id, metric_type),
    INDEX idx_time_window (time_window_start, time_window_end),
    UNIQUE KEY unique_campaign_metric_time (campaign_id, metric_type, time_window_start, time_window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
