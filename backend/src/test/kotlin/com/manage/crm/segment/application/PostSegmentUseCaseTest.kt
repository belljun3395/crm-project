package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.journey.queue.JourneyTriggerQueuePublisher
import com.manage.crm.segment.application.dto.PostSegmentConditionIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseIn
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class PostSegmentUseCaseTest : BehaviorSpec({
    lateinit var segmentRepository: SegmentRepository
    lateinit var segmentConditionRepository: SegmentConditionRepository
    lateinit var journeyTriggerQueuePublisher: JourneyTriggerQueuePublisher
    lateinit var transactionSynchronizationTemplate: TransactionSynchronizationTemplate
    lateinit var useCase: PostSegmentUseCase

    beforeTest {
        segmentRepository = mockk()
        segmentConditionRepository = mockk(relaxed = true)
        journeyTriggerQueuePublisher = mockk(relaxed = true)
        transactionSynchronizationTemplate = mockk(relaxed = true)
        useCase = PostSegmentUseCase(
            segmentRepository = segmentRepository,
            segmentConditionRepository = segmentConditionRepository,
            journeyTriggerQueuePublisher = journeyTriggerQueuePublisher,
            transactionSynchronizationTemplate = transactionSynchronizationTemplate
        )
    }

    given("post segment") {
        `when`("segment name already exists on create") {
            then("throw duplicate exception") {
                val request = PostSegmentUseCaseIn(
                    name = "existing-segment",
                    description = "desc",
                    active = true,
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "user.email",
                            operator = "EQ",
                            valueType = "STRING",
                            value = jacksonObjectMapper().readTree("\"a@b.com\"")
                        )
                    )
                )
                coEvery { segmentRepository.findByName("existing-segment") } returns Segment.new(
                    id = 1L,
                    name = "existing-segment",
                    description = "already exists",
                    active = true
                )

                shouldThrow<AlreadyExistsException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("condition operator is invalid for type") {
            then("throw invalid condition exception") {
                val request = PostSegmentUseCaseIn(
                    name = "invalid-segment",
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "user.email",
                            operator = "GT",
                            valueType = "STRING",
                            value = jacksonObjectMapper().readTree("\"a@b.com\"")
                        )
                    )
                )
                coEvery { segmentRepository.findByName("invalid-segment") } returns null

                shouldThrow<InvalidSegmentConditionException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("condition valueType does not match field") {
            then("throw invalid condition exception") {
                val request = PostSegmentUseCaseIn(
                    name = "invalid-segment",
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "user.id",
                            operator = "EQ",
                            valueType = "STRING",
                            value = jacksonObjectMapper().readTree("\"100\"")
                        )
                    )
                )
                coEvery { segmentRepository.findByName("invalid-segment") } returns null

                shouldThrow<InvalidSegmentConditionException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("datetime condition has invalid format") {
            then("throw invalid condition exception") {
                val request = PostSegmentUseCaseIn(
                    name = "invalid-segment",
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "event.occurredAt",
                            operator = "EQ",
                            valueType = "DATETIME",
                            value = jacksonObjectMapper().readTree("\"not-a-date\"")
                        )
                    )
                )
                coEvery { segmentRepository.findByName("invalid-segment") } returns null

                shouldThrow<InvalidSegmentConditionException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("condition list is empty") {
            then("throw invalid condition exception") {
                val request = PostSegmentUseCaseIn(
                    name = "invalid-segment",
                    conditions = emptyList()
                )

                shouldThrow<InvalidSegmentConditionException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("concurrent duplicate insert happens during save") {
            then("translate integrity violation to already-exists error") {
                val request = PostSegmentUseCaseIn(
                    name = "active-users",
                    description = "active users segment",
                    active = true,
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "user.email",
                            operator = "EQ",
                            valueType = "STRING",
                            value = jacksonObjectMapper().readTree("\"a@okestro.com\"")
                        )
                    )
                )

                coEvery { segmentRepository.findByName("active-users") } returns null
                coEvery { segmentRepository.save(any()) } throws DataIntegrityViolationException("Duplicate entry for key 'uq_segments_name'")

                shouldThrow<AlreadyExistsException> {
                    useCase.execute(request)
                }
            }
        }

        `when`("valid segment request is provided") {
            then("save segment and conditions") {
                val objectMapper = jacksonObjectMapper()
                val request = PostSegmentUseCaseIn(
                    name = "active-users",
                    description = "active users segment",
                    active = true,
                    conditions = listOf(
                        PostSegmentConditionIn(
                            field = "user.email",
                            operator = "CONTAINS",
                            valueType = "STRING",
                            value = objectMapper.readTree("\"@okestro.com\"")
                        ),
                        PostSegmentConditionIn(
                            field = "user.id",
                            operator = "GT",
                            valueType = "NUMBER",
                            value = objectMapper.readTree("100")
                        )
                    )
                )

                coEvery { segmentRepository.findByName("active-users") } returns null
                coEvery { segmentRepository.save(any()) } answers {
                    firstArg<Segment>().apply {
                        id = 10L
                        createdAt = LocalDateTime.of(2024, 1, 1, 10, 0)
                    }
                }
                coEvery { segmentConditionRepository.deleteBySegmentId(10L) } returns 0L
                coEvery { segmentConditionRepository.save(any()) } answers { firstArg() }

                val result = useCase.execute(request)

                result.segment.id shouldBe 10L
                result.segment.conditions.size shouldBe 2
                result.segment.conditions[0].position shouldBe 1
                result.segment.conditions[1].position shouldBe 2
                coVerify(exactly = 1) { segmentConditionRepository.deleteBySegmentId(10L) }
                coVerify(exactly = 2) { segmentConditionRepository.save(any()) }
            }
        }
    }
})
