package com.manage.crm.event.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("campaign_dashboard_metrics")
class CampaignDashboardMetrics(
    @Id
    var id: Long? = null,
    @Column("campaign_id")
    var campaignId: Long,
    @Column("metric_type")
    var metricType: MetricType,
    @Column("metric_value")
    var metricValue: Long,
    @Column("time_window_start")
    var timeWindowStart: LocalDateTime,
    @Column("time_window_end")
    var timeWindowEnd: LocalDateTime,
    @Column("time_window_unit")
    var timeWindowUnit: TimeWindowUnit,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            campaignId: Long,
            metricType: MetricType,
            metricValue: Long,
            timeWindowStart: LocalDateTime,
            timeWindowEnd: LocalDateTime,
            timeWindowUnit: TimeWindowUnit
        ): CampaignDashboardMetrics {
            return CampaignDashboardMetrics(
                campaignId = campaignId,
                metricType = metricType,
                metricValue = metricValue,
                timeWindowStart = timeWindowStart,
                timeWindowEnd = timeWindowEnd,
                timeWindowUnit = timeWindowUnit
            )
        }
    }

    fun incrementValue(incrementBy: Long = 1) {
        this.metricValue += incrementBy
    }
}

enum class MetricType {
    EVENT_COUNT,
    UNIQUE_USER_COUNT,
    TOTAL_USER_COUNT
}

enum class TimeWindowUnit {
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH
}
