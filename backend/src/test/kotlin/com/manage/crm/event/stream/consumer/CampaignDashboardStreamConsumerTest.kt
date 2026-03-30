package com.manage.crm.event.stream.consumer

import com.manage.crm.event.event.CampaignDashboardEventFixtures
import com.manage.crm.event.service.CampaignDashboardMetricsService
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class CampaignDashboardStreamConsumerTest : BehaviorSpec({
    lateinit var campaignStreamRegistryManager: CampaignStreamRegistryManager
    lateinit var campaignDashboardStreamManager: CampaignDashboardStreamManager
    lateinit var campaignDashboardMetricsService: CampaignDashboardMetricsService
    lateinit var consumer: CampaignDashboardStreamConsumer

    beforeContainer {
        campaignStreamRegistryManager = mockk()
        campaignDashboardStreamManager = mockk()
        campaignDashboardMetricsService = mockk()
        consumer = CampaignDashboardStreamConsumer(
            campaignStreamRegistryManager = campaignStreamRegistryManager,
            campaignDashboardStreamManager = campaignDashboardStreamManager,
            campaignDashboardMetricsService = campaignDashboardMetricsService
        )
    }

    given("CampaignDashboardStreamConsumer") {
        `when`("no active campaigns") {
            coEvery { campaignStreamRegistryManager.getActiveCampaigns() } returns emptySet()

            consumer.processStreamEvents()

            then("no stream reads are performed") {
                coVerify(exactly = 0) {
                    campaignDashboardStreamManager.readEventsBatch(any(), any())
                }
            }
        }

        `when`("active campaign has new events") {
            val campaignId = 1L
            val events = listOf(
                CampaignDashboardEventFixtures.aCampaignDashboardEvent()
                    .withCampaignId(campaignId)
                    .withEventId(1L)
                    .withUserId(10L)
                    .withEventName("purchase")
                    .withTimestamp(LocalDateTime.now())
                    .withStreamId("1-1")
                    .build(),
                CampaignDashboardEventFixtures.aCampaignDashboardEvent()
                    .withCampaignId(campaignId)
                    .withEventId(2L)
                    .withUserId(11L)
                    .withEventName("purchase")
                    .withTimestamp(LocalDateTime.now())
                    .withStreamId("1-2")
                    .build()
            )

            coEvery { campaignStreamRegistryManager.getActiveCampaigns() } returns setOf(campaignId)
            coEvery { campaignStreamRegistryManager.getLastProcessedId(campaignId) } returns null
            coEvery { campaignDashboardStreamManager.readEventsBatch(campaignId, null) } returns events
            coJustRun { campaignDashboardMetricsService.updateMetricsForEvents(events) }
            coJustRun { campaignStreamRegistryManager.updateLastProcessedId(campaignId, "1-2") }
            coEvery { campaignDashboardStreamManager.getStreamLength(campaignId) } returns 100L

            consumer.processStreamEvents()

            then("updates metrics for retrieved events") {
                coVerify(exactly = 1) { campaignDashboardMetricsService.updateMetricsForEvents(events) }
            }

            then("advances cursor to last processed event id") {
                coVerify(exactly = 1) { campaignStreamRegistryManager.updateLastProcessedId(campaignId, "1-2") }
            }

            then("does not trim stream when below max length") {
                coVerify(exactly = 0) { campaignDashboardStreamManager.trimStream(any(), any()) }
            }
        }

        `when`("stream length exceeds max (10000)") {
            val campaignId = 2L
            val events = listOf(
                CampaignDashboardEventFixtures.aCampaignDashboardEvent()
                    .withCampaignId(campaignId)
                    .withEventId(1L)
                    .withUserId(10L)
                    .withEventName("e")
                    .withTimestamp(LocalDateTime.now())
                    .withStreamId("2-1")
                    .build()
            )

            coEvery { campaignStreamRegistryManager.getActiveCampaigns() } returns setOf(campaignId)
            coEvery { campaignStreamRegistryManager.getLastProcessedId(campaignId) } returns null
            coEvery { campaignDashboardStreamManager.readEventsBatch(campaignId, null) } returns events
            coJustRun { campaignDashboardMetricsService.updateMetricsForEvents(events) }
            coJustRun { campaignStreamRegistryManager.updateLastProcessedId(any(), any()) }
            coEvery { campaignDashboardStreamManager.getStreamLength(campaignId) } returns 15_000L
            coJustRun { campaignDashboardStreamManager.trimStream(campaignId, 10_000L) }

            consumer.processStreamEvents()

            then("trims stream to max length") {
                coVerify(exactly = 1) { campaignDashboardStreamManager.trimStream(campaignId, 10_000L) }
            }
        }

        `when`("stream has no new events after cursor") {
            val campaignId = 3L

            coEvery { campaignStreamRegistryManager.getActiveCampaigns() } returns setOf(campaignId)
            coEvery { campaignStreamRegistryManager.getLastProcessedId(campaignId) } returns "3-99"
            coEvery { campaignDashboardStreamManager.readEventsBatch(campaignId, "3-99") } returns emptyList()

            consumer.processStreamEvents()

            then("skips metric update and cursor advance") {
                coVerify(exactly = 0) { campaignDashboardMetricsService.updateMetricsForEvents(any()) }
                coVerify(exactly = 0) { campaignStreamRegistryManager.updateLastProcessedId(any(), any()) }
            }
        }

        `when`("processing one campaign fails") {
            val failingId = 10L
            val okId = 11L
            val events = listOf(
                CampaignDashboardEventFixtures.aCampaignDashboardEvent()
                    .withCampaignId(okId)
                    .withEventId(1L)
                    .withUserId(1L)
                    .withEventName("e")
                    .withTimestamp(LocalDateTime.now())
                    .withStreamId("ok-1")
                    .build()
            )

            coEvery { campaignStreamRegistryManager.getActiveCampaigns() } returns setOf(failingId, okId)
            coEvery { campaignStreamRegistryManager.getLastProcessedId(failingId) } throws RuntimeException("Redis down")
            coEvery { campaignStreamRegistryManager.getLastProcessedId(okId) } returns null
            coEvery { campaignDashboardStreamManager.readEventsBatch(okId, null) } returns events
            coJustRun { campaignDashboardMetricsService.updateMetricsForEvents(events) }
            coJustRun { campaignStreamRegistryManager.updateLastProcessedId(okId, "ok-1") }
            coEvery { campaignDashboardStreamManager.getStreamLength(okId) } returns 10L

            consumer.processStreamEvents()

            then("continues processing remaining campaigns") {
                coVerify(exactly = 1) { campaignDashboardMetricsService.updateMetricsForEvents(events) }
            }
        }
    }
})
