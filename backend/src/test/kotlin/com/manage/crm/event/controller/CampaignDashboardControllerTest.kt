package com.manage.crm.event.controller

import com.manage.crm.event.application.DeleteCampaignUseCase
import com.manage.crm.event.application.GetCampaignDashboardStreamStatusUseCase
import com.manage.crm.event.application.GetCampaignDashboardUseCase
import com.manage.crm.event.application.GetCampaignFunnelAnalyticsUseCase
import com.manage.crm.event.application.GetCampaignSegmentComparisonUseCase
import com.manage.crm.event.application.GetCampaignSummaryUseCase
import com.manage.crm.event.application.GetCampaignUseCase
import com.manage.crm.event.application.ListCampaignsUseCase
import com.manage.crm.event.application.PostCampaignUseCase
import com.manage.crm.event.application.StreamCampaignDashboardUseCase
import com.manage.crm.event.application.UpdateCampaignUseCase
import com.manage.crm.event.application.dto.FunnelStepMetricDto
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot

class CampaignDashboardControllerTest : BehaviorSpec({
    lateinit var listCampaignsUseCase: ListCampaignsUseCase
    lateinit var getCampaignUseCase: GetCampaignUseCase
    lateinit var updateCampaignUseCase: UpdateCampaignUseCase
    lateinit var deleteCampaignUseCase: DeleteCampaignUseCase
    lateinit var streamCampaignDashboardUseCase: StreamCampaignDashboardUseCase
    lateinit var getCampaignDashboardUseCase: GetCampaignDashboardUseCase
    lateinit var getCampaignFunnelAnalyticsUseCase: GetCampaignFunnelAnalyticsUseCase
    lateinit var getCampaignSegmentComparisonUseCase: GetCampaignSegmentComparisonUseCase
    lateinit var getCampaignSummaryUseCase: GetCampaignSummaryUseCase
    lateinit var getCampaignDashboardStreamStatusUseCase: GetCampaignDashboardStreamStatusUseCase
    lateinit var postCampaignUseCase: PostCampaignUseCase
    lateinit var controller: CampaignDashboardController

    beforeContainer {
        listCampaignsUseCase = mockk()
        getCampaignUseCase = mockk()
        updateCampaignUseCase = mockk()
        deleteCampaignUseCase = mockk()
        streamCampaignDashboardUseCase = mockk()
        getCampaignDashboardUseCase = mockk()
        getCampaignFunnelAnalyticsUseCase = mockk()
        getCampaignSegmentComparisonUseCase = mockk()
        getCampaignSummaryUseCase = mockk()
        getCampaignDashboardStreamStatusUseCase = mockk()
        postCampaignUseCase = mockk()
        controller = CampaignDashboardController(
            listCampaignsUseCase = listCampaignsUseCase,
            getCampaignUseCase = getCampaignUseCase,
            updateCampaignUseCase = updateCampaignUseCase,
            deleteCampaignUseCase = deleteCampaignUseCase,
            streamCampaignDashboardUseCase = streamCampaignDashboardUseCase,
            getCampaignDashboardUseCase = getCampaignDashboardUseCase,
            getCampaignFunnelAnalyticsUseCase = getCampaignFunnelAnalyticsUseCase,
            getCampaignSegmentComparisonUseCase = getCampaignSegmentComparisonUseCase,
            getCampaignSummaryUseCase = getCampaignSummaryUseCase,
            getCampaignDashboardStreamStatusUseCase = getCampaignDashboardStreamStatusUseCase,
            postCampaignUseCase = postCampaignUseCase
        )
    }

    given("CampaignDashboardController funnel analytics") {
        `when`("steps query param is comma-separated string") {
            val useCaseInSlot = slot<GetCampaignFunnelAnalyticsUseCaseIn>()
            coEvery { getCampaignFunnelAnalyticsUseCase.execute(capture(useCaseInSlot)) } returns
                GetCampaignFunnelAnalyticsUseCaseOut(
                    campaignId = 1L,
                    stepMetrics = listOf(
                        FunnelStepMetricDto("view", 10, 10, 100.0),
                        FunnelStepMetricDto("click", 5, 5, 50.0)
                    )
                )

            controller.getCampaignFunnelAnalytics(
                campaignId = 1L,
                steps = "view, click",
                startTime = null,
                endTime = null
            )

            then("parses and trims steps into list") {
                useCaseInSlot.captured.steps shouldBe listOf("view", "click")
            }
        }

        `when`("steps string contains blank entries") {
            val useCaseInSlot = slot<GetCampaignFunnelAnalyticsUseCaseIn>()
            coEvery { getCampaignFunnelAnalyticsUseCase.execute(capture(useCaseInSlot)) } returns
                GetCampaignFunnelAnalyticsUseCaseOut(campaignId = 1L, stepMetrics = emptyList())

            controller.getCampaignFunnelAnalytics(
                campaignId = 1L,
                steps = "view,,click,",
                startTime = null,
                endTime = null
            )

            then("blank entries are filtered out") {
                useCaseInSlot.captured.steps shouldBe listOf("view", "click")
            }
        }
    }

    given("CampaignDashboardController segment comparison") {
        `when`("segmentIds query param is comma-separated string with valid longs") {
            val useCaseInSlot = slot<GetCampaignSegmentComparisonUseCaseIn>()
            coEvery { getCampaignSegmentComparisonUseCase.execute(capture(useCaseInSlot)) } returns
                GetCampaignSegmentComparisonUseCaseOut(
                    campaignId = 1L,
                    eventName = null,
                    segmentMetrics = listOf(
                        SegmentComparisonMetricDto(1L, "s1", 10, 5, 5, 50.0),
                        SegmentComparisonMetricDto(2L, "s2", 10, 3, 3, 30.0)
                    )
                )

            controller.getCampaignSegmentComparison(
                campaignId = 1L,
                segmentIds = "1,2",
                eventName = null,
                startTime = null,
                endTime = null
            )

            then("parses segment ids into list of longs") {
                useCaseInSlot.captured.segmentIds shouldBe listOf(1L, 2L)
            }
        }

        `when`("segmentIds contains non-numeric entries") {
            val useCaseInSlot = slot<GetCampaignSegmentComparisonUseCaseIn>()
            coEvery { getCampaignSegmentComparisonUseCase.execute(capture(useCaseInSlot)) } returns
                GetCampaignSegmentComparisonUseCaseOut(campaignId = 1L, eventName = null, segmentMetrics = emptyList())

            controller.getCampaignSegmentComparison(
                campaignId = 1L,
                segmentIds = "1,abc,2",
                eventName = null,
                startTime = null,
                endTime = null
            )

            then("non-numeric entries are dropped") {
                useCaseInSlot.captured.segmentIds shouldBe listOf(1L, 2L)
            }
        }

        `when`("segmentIds contains duplicates") {
            val useCaseInSlot = slot<GetCampaignSegmentComparisonUseCaseIn>()
            coEvery { getCampaignSegmentComparisonUseCase.execute(capture(useCaseInSlot)) } returns
                GetCampaignSegmentComparisonUseCaseOut(campaignId = 1L, eventName = null, segmentMetrics = emptyList())

            controller.getCampaignSegmentComparison(
                campaignId = 1L,
                segmentIds = "1,1,2",
                eventName = null,
                startTime = null,
                endTime = null
            )

            then("duplicates are removed") {
                useCaseInSlot.captured.segmentIds shouldBe listOf(1L, 2L)
            }
        }
    }

    given("CampaignDashboardController SSE stream lastEventId resolution") {
        `when`("lastEventId query param and header are both provided") {
            coEvery {
                streamCampaignDashboardUseCase.execute(any())
            } returns reactor.core.publisher.Flux.empty()

            controller.streamCampaignDashboard(
                campaignId = 1L,
                durationSeconds = 60L,
                lastEventId = "from-query",
                lastEventIdHeader = "from-header"
            )

            then("query param takes precedence over header") {
                coVerify {
                    streamCampaignDashboardUseCase.execute(
                        match { it.lastEventId == "from-query" }
                    )
                }
            }
        }

        `when`("only header lastEventId is provided") {
            coEvery {
                streamCampaignDashboardUseCase.execute(any())
            } returns reactor.core.publisher.Flux.empty()

            controller.streamCampaignDashboard(
                campaignId = 1L,
                durationSeconds = 60L,
                lastEventId = null,
                lastEventIdHeader = "from-header"
            )

            then("header value is used as lastEventId") {
                coVerify {
                    streamCampaignDashboardUseCase.execute(
                        match { it.lastEventId == "from-header" }
                    )
                }
            }
        }
    }
})
