package com.manage.crm.event.application

import com.manage.crm.event.application.dto.ListCampaignsUseCaseIn
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.repository.CampaignRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class ListCampaignsUseCaseTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var listCampaignsUseCase: ListCampaignsUseCase

    beforeContainer {
        campaignRepository = mockk()
        listCampaignsUseCase = ListCampaignsUseCase(campaignRepository)
    }

    given("UC-CAMPAIGN-002 ListCampaignsUseCase") {
        `when`("limit is greater than max bound") {
            val campaignWithId = CampaignFixtures.giveMeOne().withId(1L).withName("c1").build()
            val campaignWithoutId = CampaignFixtures.giveMeOne().withId(-1L).withName("c2").build().apply { id = null }
            val anotherCampaign = CampaignFixtures.giveMeOne().withId(3L).withName("c3").build()
            every { campaignRepository.findRecentCampaigns(1000) } returns flowOf(
                campaignWithId,
                campaignWithoutId,
                anotherCampaign
            )

            val result = listCampaignsUseCase.execute(ListCampaignsUseCaseIn(limit = 5000))

            then("should clamp limit and filter campaigns without id") {
                result.campaigns shouldHaveSize 2
                result.campaigns[0].id shouldBe 1L
                result.campaigns[1].id shouldBe 3L
                verify(exactly = 1) { campaignRepository.findRecentCampaigns(1000) }
            }
        }

        `when`("limit is less than min bound") {
            every { campaignRepository.findRecentCampaigns(1) } returns emptyFlow()

            val result = listCampaignsUseCase.execute(ListCampaignsUseCaseIn(limit = 0))

            then("should clamp to minimum limit") {
                result.campaigns shouldHaveSize 0
                verify(exactly = 1) { campaignRepository.findRecentCampaigns(1) }
            }
        }
    }
})
