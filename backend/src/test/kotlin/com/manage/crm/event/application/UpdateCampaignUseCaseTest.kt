package com.manage.crm.event.application

import com.manage.crm.event.application.dto.CampaignPropertyUseCaseDto
import com.manage.crm.event.application.dto.UpdateCampaignUseCaseIn
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.CampaignSegments
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers

class UpdateCampaignUseCaseTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var campaignSegmentsRepository: CampaignSegmentsRepository
    lateinit var segmentRepository: SegmentRepository
    lateinit var transactionSynchronizationTemplate: TransactionSynchronizationTemplate
    lateinit var campaignCacheManager: CampaignCacheManager
    lateinit var updateCampaignUseCase: UpdateCampaignUseCase

    beforeContainer {
        campaignRepository = mockk()
        campaignSegmentsRepository = mockk(relaxed = true)
        segmentRepository = mockk()
        transactionSynchronizationTemplate = mockk()
        campaignCacheManager = mockk()
        updateCampaignUseCase = UpdateCampaignUseCase(
            campaignRepository = campaignRepository,
            campaignSegmentsRepository = campaignSegmentsRepository,
            segmentRepository = segmentRepository,
            transactionSynchronizationTemplate = transactionSynchronizationTemplate,
            campaignCacheManager = campaignCacheManager
        )
    }

    given("UC-CAMPAIGN-004: UpdateCampaignUseCase") {
        `when`("campaign is updated with new name and properties") {
            val campaignId = 1L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("old-name").build()
            val input = UpdateCampaignUseCaseIn(
                campaignId = campaignId,
                name = "new-name",
                properties = listOf(CampaignPropertyUseCaseDto("k", "v"))
            )

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("new-name") } returns null
            coEvery { campaignRepository.save(any()) } answers { firstArg() }
            coEvery { campaignSegmentsRepository.findAllByCampaignId(campaignId) } returns emptyList()
            coEvery {
                transactionSynchronizationTemplate.afterCommit(
                    eq(Dispatchers.IO),
                    eq("refresh campaign cache after update"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers { lambda<suspend () -> Unit>().coInvoke() }
            coJustRun { campaignCacheManager.evict(any(), any()) }
            coEvery { campaignCacheManager.save(any()) } answers { firstArg() }

            val result = updateCampaignUseCase.execute(input)

            then("returns updated campaign detail") {
                result.id shouldBe campaignId
                result.name shouldBe "new-name"
                result.properties.size shouldBe 1
            }

            then("saves updated campaign") {
                coVerify(exactly = 1) { campaignRepository.save(any()) }
            }

            then("refreshes cache after commit") {
                coVerify(exactly = 1) { campaignCacheManager.evict(campaignId, "old-name") }
                coVerify(exactly = 1) { campaignCacheManager.save(any()) }
            }
        }

        `when`("campaign does not exist") {
            val campaignId = 99L
            coEvery { campaignRepository.findById(campaignId) } returns null

            then("throws NotFoundByIdException") {
                shouldThrow<NotFoundByIdException> {
                    updateCampaignUseCase.execute(
                        UpdateCampaignUseCaseIn(campaignId, "any", emptyList())
                    )
                }
            }

            then("does not save") {
                coVerify(exactly = 0) { campaignRepository.save(any()) }
            }
        }

        `when`("new name is already taken by another campaign") {
            val campaignId = 2L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("my-campaign").build()
            val other = CampaignFixtures.giveMeOne().withId(999L).withName("taken-name").build()

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("taken-name") } returns other

            then("throws AlreadyExistsException") {
                shouldThrow<AlreadyExistsException> {
                    updateCampaignUseCase.execute(
                        UpdateCampaignUseCaseIn(campaignId, "taken-name", emptyList())
                    )
                }
            }
        }

        `when`("same name is kept (self-update)") {
            val campaignId = 3L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("same-name").build()

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("same-name") } returns existing
            coEvery { campaignRepository.save(any()) } answers { firstArg() }
            coEvery { campaignSegmentsRepository.findAllByCampaignId(campaignId) } returns emptyList()
            coEvery {
                transactionSynchronizationTemplate.afterCommit(
                    eq(Dispatchers.IO),
                    eq("refresh campaign cache after update"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers { lambda<suspend () -> Unit>().coInvoke() }
            coJustRun { campaignCacheManager.evict(any(), any()) }
            coEvery { campaignCacheManager.save(any()) } answers { firstArg() }

            then("update succeeds without AlreadyExistsException") {
                val result = updateCampaignUseCase.execute(
                    UpdateCampaignUseCaseIn(campaignId, "same-name", emptyList())
                )
                result.id shouldBe campaignId
            }
        }

        `when`("segment ids are provided but a segment does not exist") {
            val campaignId = 4L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("camp").build()

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("camp") } returns null
            coEvery { segmentRepository.findById(777L) } returns null

            then("throws NotFoundByIdException for missing segment") {
                shouldThrow<NotFoundByIdException> {
                    updateCampaignUseCase.execute(
                        UpdateCampaignUseCaseIn(campaignId, "camp", emptyList(), segmentIds = listOf(777L))
                    )
                }
            }

            then("does not save campaign") {
                coVerify(exactly = 0) { campaignRepository.save(any()) }
            }
        }

        `when`("segmentIds is null — existing segments are preserved") {
            val campaignId = 6L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("keep-segs").build()
            val existingSegmentIds = listOf(30L, 40L)

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("keep-segs") } returns null
            coEvery { campaignRepository.save(any()) } answers { firstArg() }
            coEvery { campaignSegmentsRepository.findAllByCampaignId(campaignId) } returns
                existingSegmentIds.map {
                    com.manage.crm.event.domain.CampaignSegments.new(campaignId = campaignId, segmentId = it)
                }
            coEvery {
                transactionSynchronizationTemplate.afterCommit(
                    eq(Dispatchers.IO),
                    eq("refresh campaign cache after update"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers { lambda<suspend () -> Unit>().coInvoke() }
            coJustRun { campaignCacheManager.evict(any(), any()) }
            coEvery { campaignCacheManager.save(any()) } answers { firstArg() }

            val result = updateCampaignUseCase.execute(
                UpdateCampaignUseCaseIn(campaignId, "keep-segs", emptyList(), segmentIds = null)
            )

            then("does not replace segments") {
                coVerify(exactly = 0) { campaignSegmentsRepository.deleteAllByCampaignId(any()) }
            }

            then("returns existing segment ids") {
                result.segmentIds shouldBe existingSegmentIds
            }
        }

        `when`("input name has leading and trailing whitespace") {
            val campaignId = 7L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("old").build()

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("trimmed") } returns null
            coEvery { campaignRepository.save(any()) } answers { firstArg() }
            coEvery { campaignSegmentsRepository.findAllByCampaignId(campaignId) } returns emptyList()
            coEvery {
                transactionSynchronizationTemplate.afterCommit(
                    eq(Dispatchers.IO),
                    eq("refresh campaign cache after update"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers { lambda<suspend () -> Unit>().coInvoke() }
            coJustRun { campaignCacheManager.evict(any(), any()) }
            coEvery { campaignCacheManager.save(any()) } answers { firstArg() }

            val result = updateCampaignUseCase.execute(
                UpdateCampaignUseCaseIn(campaignId, "  trimmed  ", emptyList())
            )

            then("name is trimmed before saving") {
                result.name shouldBe "trimmed"
            }
        }

        `when`("segment ids are provided and all segments exist") {
            val campaignId = 5L
            val existing = CampaignFixtures.giveMeOne().withId(campaignId).withName("seg-camp").build()

            coEvery { campaignRepository.findById(campaignId) } returns existing
            coEvery { campaignRepository.findCampaignByName("seg-camp") } returns null
            coEvery { segmentRepository.findById(10L) } returns Segment.new(10L, "s1", null, true)
            coEvery { segmentRepository.findById(20L) } returns Segment.new(20L, "s2", null, true)
            coEvery { campaignRepository.save(any()) } answers { firstArg() }
            coEvery {
                transactionSynchronizationTemplate.afterCommit(
                    eq(Dispatchers.IO),
                    eq("refresh campaign cache after update"),
                    captureLambda<suspend () -> Unit>()
                )
            } coAnswers { lambda<suspend () -> Unit>().coInvoke() }
            coJustRun { campaignCacheManager.evict(any(), any()) }
            coEvery { campaignCacheManager.save(any()) } answers { firstArg() }

            val result = updateCampaignUseCase.execute(
                UpdateCampaignUseCaseIn(campaignId, "seg-camp", emptyList(), segmentIds = listOf(10L, 20L))
            )

            then("replaces campaign segments") {
                coVerify(exactly = 1) { campaignSegmentsRepository.deleteAllByCampaignId(campaignId) }
                coVerify(exactly = 2) { campaignSegmentsRepository.save(any()) }
            }

            then("returns segment ids in result") {
                result.segmentIds shouldBe listOf(10L, 20L)
            }
        }
    }
})
