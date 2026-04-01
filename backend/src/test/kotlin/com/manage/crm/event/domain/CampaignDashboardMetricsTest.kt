package com.manage.crm.event.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CampaignDashboardMetricsTest :
    BehaviorSpec({
        val windowStart = LocalDateTime.of(2026, 3, 1, 10, 0)
        val windowEnd = LocalDateTime.of(2026, 3, 1, 11, 0)

        given("CampaignDashboardMetrics#incrementValue") {
            `when`("called with default increment") {
                val metrics =
                    CampaignDashboardMetrics.new(
                        campaignId = 1L,
                        metricType = MetricType.EVENT_COUNT,
                        metricValue = 5L,
                        timeWindowStart = windowStart,
                        timeWindowEnd = windowEnd,
                        timeWindowUnit = TimeWindowUnit.HOUR,
                    )

                metrics.incrementValue()

                then("increases metricValue by 1") {
                    metrics.metricValue shouldBe 6L
                }
            }

            `when`("called with explicit increment amount") {
                val metrics =
                    CampaignDashboardMetrics.new(
                        campaignId = 1L,
                        metricType = MetricType.EVENT_COUNT,
                        metricValue = 10L,
                        timeWindowStart = windowStart,
                        timeWindowEnd = windowEnd,
                        timeWindowUnit = TimeWindowUnit.HOUR,
                    )

                metrics.incrementValue(incrementBy = 5L)

                then("increases metricValue by the specified amount") {
                    metrics.metricValue shouldBe 15L
                }
            }

            `when`("called multiple times") {
                val metrics =
                    CampaignDashboardMetrics.new(
                        campaignId = 1L,
                        metricType = MetricType.EVENT_COUNT,
                        metricValue = 0L,
                        timeWindowStart = windowStart,
                        timeWindowEnd = windowEnd,
                        timeWindowUnit = TimeWindowUnit.HOUR,
                    )

                metrics.incrementValue()
                metrics.incrementValue()
                metrics.incrementValue()

                then("accumulates correctly") {
                    metrics.metricValue shouldBe 3L
                }
            }
        }
    })
