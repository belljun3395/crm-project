package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.service.CampaignEventsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime

class GetCampaignFunnelAnalyticsUseCaseTest :
    BehaviorSpec({
        lateinit var campaignEventsService: CampaignEventsService
        lateinit var getCampaignFunnelAnalyticsUseCase: GetCampaignFunnelAnalyticsUseCase

        beforeContainer {
            campaignEventsService = mockk()
            getCampaignFunnelAnalyticsUseCase = GetCampaignFunnelAnalyticsUseCase(campaignEventsService)
        }

        given("UC-CAMPAIGN-009: GetCampaignFunnelAnalyticsUseCase") {
            `when`("fewer than two funnel steps are provided") {
                then("throws IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        getCampaignFunnelAnalyticsUseCase.execute(
                            GetCampaignFunnelAnalyticsUseCaseIn(
                                campaignId = 1L,
                                steps = listOf("only-one"),
                                startTime = null,
                                endTime = null,
                            ),
                        )
                    }
                }
            }

            `when`("users complete all funnel steps in order") {
                val now = LocalDateTime.now()
                val events =
                    listOf(
                        event(1L, 101L, "view", now.plusMinutes(1)),
                        event(2L, 101L, "click", now.plusMinutes(2)),
                        event(3L, 101L, "purchase", now.plusMinutes(3)),
                        event(4L, 102L, "view", now.plusMinutes(1)),
                        event(5L, 102L, "click", now.plusMinutes(2)),
                        event(6L, 102L, "purchase", now.plusMinutes(3)),
                    )

                coEvery { campaignEventsService.findCampaignEvents(1L, null, null) } returns events

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(1L, listOf("view", "click", "purchase"), null, null),
                    )

                then("all users qualify at each step") {
                    result.stepMetrics[0].qualifiedUserCount shouldBe 2
                    result.stepMetrics[1].qualifiedUserCount shouldBe 2
                    result.stepMetrics[2].qualifiedUserCount shouldBe 2
                }

                then("conversion from first step is 100%") {
                    result.stepMetrics[0].conversionFromPrevious shouldBe 100.0
                }
            }

            `when`("users drop off mid-funnel") {
                val now = LocalDateTime.now()
                val events =
                    listOf(
                        event(1L, 101L, "view", now.plusMinutes(1)),
                        event(2L, 101L, "click", now.plusMinutes(2)),
                        event(3L, 102L, "view", now.plusMinutes(1)),
                        // user 102 does not reach click
                    )

                coEvery { campaignEventsService.findCampaignEvents(2L, null, null) } returns events

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(2L, listOf("view", "click"), null, null),
                    )

                then("first step includes all users") {
                    result.stepMetrics[0].qualifiedUserCount shouldBe 2
                }

                then("second step counts only users who reached click") {
                    result.stepMetrics[1].qualifiedUserCount shouldBe 1
                }

                then("conversion from view to click is 50%") {
                    result.stepMetrics[1].conversionFromPrevious shouldBe 50.0
                }
            }

            `when`("no events exist for the campaign") {
                coEvery { campaignEventsService.findCampaignEvents(3L, null, null) } returns emptyList()

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(3L, listOf("view", "click"), null, null),
                    )

                then("all step counts are zero") {
                    result.stepMetrics.forEach { metric ->
                        metric.qualifiedUserCount shouldBe 0
                        metric.eventCount shouldBe 0
                    }
                }
            }

            `when`("user events are out of order (non-sequential funnel path)") {
                val now = LocalDateTime.now()
                val events =
                    listOf(
                        event(1L, 101L, "view", now.plusMinutes(1)),
                        event(2L, 101L, "purchase", now.plusMinutes(2)), // skips click
                        event(3L, 101L, "click", now.plusMinutes(3)),
                    )

                coEvery { campaignEventsService.findCampaignEvents(4L, null, null) } returns events

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(4L, listOf("view", "click", "purchase"), null, null),
                    )

                then("user does not qualify for purchase step because click was skipped") {
                    result.stepMetrics[2].qualifiedUserCount shouldBe 0
                }

                then("user qualifies for click step reached after purchase in time") {
                    result.stepMetrics[1].qualifiedUserCount shouldBe 1
                }
            }

            `when`("same user fires the same step event multiple times") {
                val now = LocalDateTime.now()
                val events =
                    listOf(
                        event(1L, 101L, "view", now.plusMinutes(1)),
                        event(2L, 101L, "view", now.plusMinutes(2)), // duplicate view from user 101
                        event(3L, 101L, "click", now.plusMinutes(3)),
                    )

                coEvery { campaignEventsService.findCampaignEvents(6L, null, null) } returns events

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(6L, listOf("view", "click"), null, null),
                    )

                then("qualifiedUserCount is 1 (deduplicated) but eventCount includes duplicates") {
                    result.stepMetrics[0].qualifiedUserCount shouldBe 1
                    result.stepMetrics[0].eventCount shouldBe 2
                    result.stepMetrics[1].qualifiedUserCount shouldBe 1
                }
            }

            `when`("steps contain leading and trailing whitespace") {
                coEvery { campaignEventsService.findCampaignEvents(7L, null, null) } returns emptyList()

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(7L, listOf(" view ", " click "), null, null),
                    )

                then("steps are trimmed and blank entries are removed") {
                    result.stepMetrics.map { it.step } shouldBe listOf("view", "click")
                }
            }

            `when`("time range filter is applied") {
                val start = LocalDateTime.of(2026, 1, 1, 0, 0)
                val end = LocalDateTime.of(2026, 1, 2, 0, 0)
                coEvery { campaignEventsService.findCampaignEvents(5L, start, end) } returns emptyList()

                val result =
                    getCampaignFunnelAnalyticsUseCase.execute(
                        GetCampaignFunnelAnalyticsUseCaseIn(5L, listOf("view", "click"), start, end),
                    )

                then("delegates time range to service and returns metrics for empty result") {
                    result.campaignId shouldBe 5L
                    result.stepMetrics.size shouldBe 2
                }
            }
        }
    })

private fun event(
    id: Long,
    userId: Long,
    name: String,
    createdAt: LocalDateTime,
): Event = Event.new(id, name, userId, PropertiesFixtures.giveMeOne().buildEvent(), createdAt)
