package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetStreamStatusUseCaseIn
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class GetCampaignDashboardStreamStatusUseCaseTest : BehaviorSpec({
    lateinit var campaignDashboardStreamManager: CampaignDashboardStreamManager
    lateinit var getCampaignDashboardStreamStatusUseCase: GetCampaignDashboardStreamStatusUseCase

    beforeContainer {
        campaignDashboardStreamManager = mockk()
        getCampaignDashboardStreamStatusUseCase =
            GetCampaignDashboardStreamStatusUseCase(campaignDashboardStreamManager)
    }

    given("UC-CAMPAIGN-011: GetCampaignDashboardStreamStatusUseCase") {
        `when`("stream exists with events") {
            val campaignId = 1L
            coEvery { campaignDashboardStreamManager.getStreamLength(campaignId) } returns 42L

            val result = getCampaignDashboardStreamStatusUseCase.execute(GetStreamStatusUseCaseIn(campaignId))

            then("returns campaign id and stream length") {
                result.campaignId shouldBe campaignId
                result.streamLength shouldBe 42L
            }

            then("includes checked timestamp") {
                result.checkedAt shouldNotBe null
            }

            then("delegates to stream manager") {
                coVerify(exactly = 1) { campaignDashboardStreamManager.getStreamLength(campaignId) }
            }
        }

        `when`("stream is empty or does not exist") {
            val campaignId = 2L
            coEvery { campaignDashboardStreamManager.getStreamLength(campaignId) } returns 0L

            val result = getCampaignDashboardStreamStatusUseCase.execute(GetStreamStatusUseCaseIn(campaignId))

            then("returns stream length of 0") {
                result.streamLength shouldBe 0L
            }
        }
    }
})
