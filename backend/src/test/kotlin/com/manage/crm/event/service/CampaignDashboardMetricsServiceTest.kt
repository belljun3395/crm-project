package com.manage.crm.event.service

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.event.CampaignDashboardEventFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class CampaignDashboardMetricsServiceTest :
    BehaviorSpec({
        lateinit var campaignEventsRepository: CampaignEventsRepository
        lateinit var campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
        lateinit var campaignDashboardMetricsService: CampaignDashboardMetricsService

        beforeContainer {
            campaignEventsRepository = mockk()
            campaignDashboardMetricsRepository = mockk()
            campaignDashboardMetricsService =
                CampaignDashboardMetricsService(
                    campaignEventsRepository = campaignEventsRepository,
                    campaignDashboardMetricsRepository = campaignDashboardMetricsRepository,
                )
        }

        given("CampaignDashboardMetricsService") {
            `when`("updateMetricsForEvents with a single event") {
                val timestamp = LocalDateTime.of(2026, 3, 15, 10, 30, 0)
                val event =
                    CampaignDashboardEventFixtures
                        .aCampaignDashboardEvent()
                        .withCampaignId(1L)
                        .withEventId(100L)
                        .withUserId(200L)
                        .withEventName("purchase")
                        .withTimestamp(timestamp)
                        .build()

                coJustRun {
                    campaignDashboardMetricsRepository.upsertMetric(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
                coJustRun {
                    campaignDashboardMetricsRepository.upsertMetricAbsolute(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
                coEvery { campaignEventsRepository.countEventsByCampaignIdAndCreatedAtRange(any(), any(), any()) } returns 5L
                coEvery {
                    campaignEventsRepository.countDistinctUsersByCampaignIdAndCreatedAtRange(any(), any(), any())
                } returns 3L

                campaignDashboardMetricsService.updateMetricsForEvents(listOf(event))

                then("upserts EVENT_COUNT for all 5 time window units") {
                    coVerify(exactly = 5) {
                        campaignDashboardMetricsRepository.upsertMetric(
                            campaignId = 1L,
                            metricType = MetricType.EVENT_COUNT,
                            metricValue = any(),
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = any(),
                        )
                    }
                }

                then("upserts TOTAL_USER_COUNT and UNIQUE_USER_COUNT for all 5 time window units") {
                    coVerify(exactly = 5) {
                        campaignDashboardMetricsRepository.upsertMetricAbsolute(
                            campaignId = 1L,
                            metricType = MetricType.TOTAL_USER_COUNT,
                            metricValue = any(),
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = any(),
                        )
                    }
                    coVerify(exactly = 5) {
                        campaignDashboardMetricsRepository.upsertMetricAbsolute(
                            campaignId = 1L,
                            metricType = MetricType.UNIQUE_USER_COUNT,
                            metricValue = any(),
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = any(),
                        )
                    }
                }
            }

            `when`("events span two campaigns") {
                val timestamp = LocalDateTime.of(2026, 3, 15, 10, 0, 0)
                val events =
                    listOf(
                        CampaignDashboardEventFixtures
                            .aCampaignDashboardEvent()
                            .withCampaignId(1L)
                            .withEventId(1L)
                            .withUserId(10L)
                            .withEventName("e")
                            .withTimestamp(timestamp)
                            .build(),
                        CampaignDashboardEventFixtures
                            .aCampaignDashboardEvent()
                            .withCampaignId(2L)
                            .withEventId(2L)
                            .withUserId(20L)
                            .withEventName("e")
                            .withTimestamp(timestamp)
                            .build(),
                    )

                coJustRun { campaignDashboardMetricsRepository.upsertMetric(any(), any(), any(), any(), any(), any()) }
                coJustRun { campaignDashboardMetricsRepository.upsertMetricAbsolute(any(), any(), any(), any(), any(), any()) }
                coEvery { campaignEventsRepository.countEventsByCampaignIdAndCreatedAtRange(any(), any(), any()) } returns 1L
                coEvery {
                    campaignEventsRepository.countDistinctUsersByCampaignIdAndCreatedAtRange(any(), any(), any())
                } returns 1L

                campaignDashboardMetricsService.updateMetricsForEvents(events)

                then("processes metrics independently for each campaign") {
                    coVerify(exactly = 5) {
                        campaignDashboardMetricsRepository.upsertMetric(
                            campaignId = 1L,
                            metricType = MetricType.EVENT_COUNT,
                            metricValue = any(),
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = any(),
                        )
                    }
                    coVerify(exactly = 5) {
                        campaignDashboardMetricsRepository.upsertMetric(
                            campaignId = 2L,
                            metricType = MetricType.EVENT_COUNT,
                            metricValue = any(),
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = any(),
                        )
                    }
                }
            }

            `when`("events list is empty") {
                campaignDashboardMetricsService.updateMetricsForEvents(emptyList())

                then("no repository calls are made") {
                    coVerify(exactly = 0) {
                        campaignDashboardMetricsRepository.upsertMetric(any(), any(), any(), any(), any(), any())
                    }
                }
            }

            `when`("two events for same campaign fall in the same time window") {
                val timestamp = LocalDateTime.of(2026, 3, 15, 10, 5, 0)
                val events =
                    listOf(
                        CampaignDashboardEventFixtures
                            .aCampaignDashboardEvent()
                            .withCampaignId(1L)
                            .withEventId(1L)
                            .withUserId(10L)
                            .withEventName("e")
                            .withTimestamp(timestamp)
                            .build(),
                        CampaignDashboardEventFixtures
                            .aCampaignDashboardEvent()
                            .withCampaignId(1L)
                            .withEventId(2L)
                            .withUserId(11L)
                            .withEventName("e")
                            .withTimestamp(timestamp.plusSeconds(30))
                            .build(),
                    )

                coJustRun { campaignDashboardMetricsRepository.upsertMetric(any(), any(), any(), any(), any(), any()) }
                coJustRun { campaignDashboardMetricsRepository.upsertMetricAbsolute(any(), any(), any(), any(), any(), any()) }
                coEvery { campaignEventsRepository.countEventsByCampaignIdAndCreatedAtRange(any(), any(), any()) } returns 2L
                coEvery {
                    campaignEventsRepository.countDistinctUsersByCampaignIdAndCreatedAtRange(any(), any(), any())
                } returns 2L

                campaignDashboardMetricsService.updateMetricsForEvents(events)

                then("upserts MINUTE window with count 2") {
                    coVerify(exactly = 1) {
                        campaignDashboardMetricsRepository.upsertMetric(
                            campaignId = 1L,
                            metricType = MetricType.EVENT_COUNT,
                            metricValue = 2L,
                            timeWindowStart = any(),
                            timeWindowEnd = any(),
                            timeWindowUnit = TimeWindowUnit.MINUTE,
                        )
                    }
                }
            }
        }
    })
