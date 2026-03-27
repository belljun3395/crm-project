package com.manage.crm.event.application

import com.manage.crm.event.application.dto.DeleteCampaignUseCaseIn
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class DeleteCampaignUseCaseTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var campaignSegmentsRepository: CampaignSegmentsRepository
    lateinit var campaignCacheManager: CampaignCacheManager
    lateinit var campaignDashboardStreamManager: CampaignDashboardStreamManager
    lateinit var campaignStreamRegistryManager: CampaignStreamRegistryManager
    lateinit var deleteCampaignUseCase: DeleteCampaignUseCase

    beforeContainer {
        campaignRepository = mockk()
        campaignSegmentsRepository = mockk()
        campaignCacheManager = mockk()
        campaignDashboardStreamManager = mockk()
        campaignStreamRegistryManager = mockk()
        deleteCampaignUseCase = DeleteCampaignUseCase(
            campaignRepository = campaignRepository,
            campaignSegmentsRepository = campaignSegmentsRepository,
            campaignCacheManager = campaignCacheManager,
            campaignDashboardStreamManager = campaignDashboardStreamManager,
            campaignStreamRegistryManager = campaignStreamRegistryManager
        )
    }

    given("UC-CAMPAIGN-005 DeleteCampaignUseCase") {
        `when`("delete existing campaign") {
            val campaignId = 10L
            val campaignName = "campaign-10"
            val campaign = CampaignFixtures.giveMeOne()
                .withId(campaignId)
                .withName(campaignName)
                .build()

            coEvery { campaignRepository.findById(campaignId) } returns campaign
            coJustRun { campaignSegmentsRepository.deleteAllByCampaignId(campaignId) }
            coJustRun { campaignRepository.deleteById(campaignId) }
            coJustRun { campaignCacheManager.evict(campaignId, campaignName) }
            coJustRun { campaignDashboardStreamManager.deleteStream(campaignId) }
            coJustRun { campaignStreamRegistryManager.unregisterCampaign(campaignId) }

            val result = deleteCampaignUseCase.execute(DeleteCampaignUseCaseIn(campaignId))

            then("should delete campaign and related side effects") {
                result.success shouldBe true
                coVerify(exactly = 1) { campaignSegmentsRepository.deleteAllByCampaignId(campaignId) }
                coVerify(exactly = 1) { campaignRepository.deleteById(campaignId) }
                coVerify(exactly = 1) { campaignCacheManager.evict(campaignId, campaignName) }
                coVerify(exactly = 1) { campaignDashboardStreamManager.deleteStream(campaignId) }
                coVerify(exactly = 1) { campaignStreamRegistryManager.unregisterCampaign(campaignId) }
            }
        }

        `when`("delete missing campaign") {
            val campaignId = 11L
            coEvery { campaignRepository.findById(campaignId) } returns null

            then("should throw not found") {
                shouldThrow<NotFoundByIdException> {
                    deleteCampaignUseCase.execute(DeleteCampaignUseCaseIn(campaignId))
                }
            }

            then("should not execute delete side effects") {
                coVerify(exactly = 0) { campaignSegmentsRepository.deleteAllByCampaignId(any()) }
                coVerify(exactly = 0) { campaignRepository.deleteById(any()) }
                coVerify(exactly = 0) { campaignCacheManager.evict(any(), any()) }
                coVerify(exactly = 0) { campaignDashboardStreamManager.deleteStream(any()) }
                coVerify(exactly = 0) { campaignStreamRegistryManager.unregisterCampaign(any()) }
            }
        }
    }
})
