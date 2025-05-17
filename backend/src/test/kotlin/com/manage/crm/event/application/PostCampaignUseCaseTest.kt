package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers

class PostCampaignUseCaseTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var transactionSynchronizationTemplate: TransactionSynchronizationTemplate
    lateinit var campaignCacheManager: CampaignCacheManager
    lateinit var postCampaignUseCase: PostCampaignUseCase

    beforeContainer {
        campaignRepository = mockk()
        transactionSynchronizationTemplate = mockk()
        campaignCacheManager = mockk()
        postCampaignUseCase =
            PostCampaignUseCase(campaignRepository, transactionSynchronizationTemplate, campaignCacheManager)
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

            coEvery { campaignCacheManager.save(any()) } answers { savedCampaign }

            coEvery {
                transactionSynchronizationTemplate.afterCompletion(
                    eq(Dispatchers.IO),
                    eq("save campaign cache"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers {
                lambda<suspend () -> Unit>().coInvoke()
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

            then("register transaction synchronization after completion for save campaign cache") {
                coVerify(exactly = 1) {
                    transactionSynchronizationTemplate.afterCompletion(
                        eq(Dispatchers.IO),
                        eq("save campaign cache"),
                        captureLambda<suspend () -> Unit>()
                    )
                }
            }

            then("save campaign cache") {
                coVerify(exactly = 1) { campaignCacheManager.save(any()) }
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

            then("not called register transaction synchronization after completion for save campaign cache") {
                coVerify(exactly = 0) {
                    transactionSynchronizationTemplate.afterCompletion(
                        eq(Dispatchers.IO),
                        eq("save campaign cache"),
                        captureLambda<suspend () -> Unit>()
                    )
                }
            }

            then("not called save campaign cache") {
                coVerify(exactly = 0) { campaignCacheManager.save(any()) }
            }
        }
    }
})
