package com.manage.crm.event.event

import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class CampaignEventPublisherTest : BehaviorSpec({
    lateinit var campaignDashboardStreamManager: CampaignDashboardStreamManager
    lateinit var campaignStreamRegistryManager: CampaignStreamRegistryManager
    lateinit var campaignEventPublisher: CampaignEventPublisher

    beforeContainer {
        campaignDashboardStreamManager = mockk()
        campaignStreamRegistryManager = mockk()
        campaignEventPublisher = CampaignEventPublisher(
            campaignDashboardStreamManager = campaignDashboardStreamManager,
            campaignStreamRegistryManager = campaignStreamRegistryManager
        )
    }

    given("CampaignEventPublisher") {
        `when`("publishCampaignEvent is called") {
            val event = CampaignDashboardEvent(
                campaignId = 1L,
                eventId = 10L,
                userId = 100L,
                eventName = "purchase",
                timestamp = LocalDateTime.now()
            )

            coJustRun { campaignDashboardStreamManager.publishEvent(event) }
            coJustRun { campaignStreamRegistryManager.registerCampaign(event.campaignId) }

            campaignEventPublisher.publishCampaignEvent(event)

            then("publishes event to stream") {
                coVerify(exactly = 1) { campaignDashboardStreamManager.publishEvent(event) }
            }

            then("registers campaign as active") {
                coVerify(exactly = 1) { campaignStreamRegistryManager.registerCampaign(event.campaignId) }
            }
        }
    }
})
