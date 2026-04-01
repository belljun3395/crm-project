package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignUseCaseIn
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.CampaignSegments
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class GetCampaignUseCaseTest :
    BehaviorSpec({
        lateinit var campaignRepository: CampaignRepository
        lateinit var campaignSegmentsRepository: CampaignSegmentsRepository
        lateinit var getCampaignUseCase: GetCampaignUseCase

        beforeContainer {
            campaignRepository = mockk()
            campaignSegmentsRepository = mockk()
            getCampaignUseCase =
                GetCampaignUseCase(
                    campaignRepository = campaignRepository,
                    campaignSegmentsRepository = campaignSegmentsRepository,
                )
        }

        given("UC-CAMPAIGN-003 GetCampaignUseCase") {
            `when`("campaign exists") {
                val campaignId = 33L
                val campaign =
                    CampaignFixtures
                        .giveMeOne()
                        .withId(campaignId)
                        .withName("campaign-33")
                        .build()
                val mappings =
                    listOf(
                        CampaignSegments.new(campaignId = campaignId, segmentId = 100L),
                        CampaignSegments.new(campaignId = campaignId, segmentId = 200L),
                    )
                coEvery { campaignRepository.findById(campaignId) } returns campaign
                coEvery { campaignSegmentsRepository.findAllByCampaignId(campaignId) } returns mappings

                val result = getCampaignUseCase.execute(GetCampaignUseCaseIn(campaignId))

                then("should return campaign details with segment ids") {
                    result.id shouldBe campaignId
                    result.name shouldBe "campaign-33"
                    result.segmentIds.shouldContainExactly(100L, 200L)
                    result.properties.size shouldBe campaign.properties.value.size
                }
            }

            `when`("campaign does not exist") {
                val campaignId = 34L
                coEvery { campaignRepository.findById(campaignId) } returns null

                then("should throw not found by id") {
                    shouldThrow<NotFoundByIdException> {
                        getCampaignUseCase.execute(GetCampaignUseCaseIn(campaignId))
                    }
                }

                then("should not query segments") {
                    coVerify(exactly = 0) { campaignSegmentsRepository.findAllByCampaignId(any()) }
                }
            }
        }
    })
