package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.exception.AlreadyExistsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.exactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class PostCampaignUseCaseTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var postCampaignUseCase: PostCampaignUseCase

    beforeContainer {
        campaignRepository = mockk()
        postCampaignUseCase = PostCampaignUseCase(campaignRepository)
    }

    given("PostCampaignUseCase") {
        `when`("post campaign") {
            val useCaseIn = PostCampaignUseCaseIn(
                name = "campaign",
                properties = listOf(
                    PostCampaignPropertyDto(
                        key = "key1",
                        value = "value1"
                    ),
                    PostCampaignPropertyDto(
                        key = "key2",
                        value = "value2"
                    )
                )
            )

            coEvery { campaignRepository.existsCampaignsByName(useCaseIn.name) } returns false

            val savedCampaign = Campaign(
                name = useCaseIn.name,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }
                )
            )
            val savedCampaignId = 1L
            coEvery { campaignRepository.save(any()) } answers {
                savedCampaign.apply {
                    id = savedCampaignId
                }
            }

            val result = postCampaignUseCase.execute(useCaseIn)

            then("should return PostCampaignUseCaseOut") {
                result.id shouldBe savedCampaignId
                result.name shouldBe useCaseIn.name
                result.properties.size shouldBe useCaseIn.properties.size
            }

            then("check campaign name is exists") {
                coVerify(exactly = 1) { campaignRepository.existsCampaignsByName(useCaseIn.name) }
            }

            then("save campaign") {
                coVerify(exactly = 1) { campaignRepository.save(any()) }
            }
        }

        `when`("post campaign with existing name") {
            val useCaseIn = PostCampaignUseCaseIn(
                name = "existing_campaign",
                properties = listOf(
                    PostCampaignPropertyDto(
                        key = "key1",
                        value = "value1"
                    )
                )
            )

            coEvery { campaignRepository.existsCampaignsByName(useCaseIn.name) } returns true

            then("should throw exception") {
                val exception = shouldThrow<AlreadyExistsException> {
                    postCampaignUseCase.execute(useCaseIn)
                }

                exception.message shouldBe "Campaign already exists with name: ${useCaseIn.name}"
            }

            then("check campaign name is exists") {
                coVerify(exactly = 1) { campaignRepository.existsCampaignsByName(useCaseIn.name) }
            }

            then("not called save campaign") {
                coVerify(exactly = 0) { campaignRepository.save(any(Campaign::class)) }
            }
        }
    }
})
