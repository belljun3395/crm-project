package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseIn
import com.manage.crm.segment.domain.SegmentConditionFixtures
import com.manage.crm.segment.domain.SegmentFixtures
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseSegmentUseCaseTest :
    BehaviorSpec({
        lateinit var segmentRepository: SegmentRepository
        lateinit var segmentConditionRepository: SegmentConditionRepository
        lateinit var useCase: BrowseSegmentUseCase

        beforeContainer {
            segmentRepository = mockk()
            segmentConditionRepository = mockk()
            useCase =
                BrowseSegmentUseCase(
                    segmentRepository = segmentRepository,
                    segmentConditionRepository = segmentConditionRepository,
                    objectMapper = jacksonObjectMapper(),
                )
        }

        given("UC-SEGMENT-002 BrowseSegmentUseCase") {
            `when`("segments exist") {
                then("return segments with grouped conditions") {
                    val firstSegment =
                        SegmentFixtures
                            .aSegment()
                            .withId(10L)
                            .withName("segment-a")
                            .withDescription("first")
                            .withActive(true)
                            .withCreatedAt(LocalDateTime.of(2024, 1, 1, 9, 0))
                            .build()

                    val secondSegment =
                        SegmentFixtures
                            .aSegment()
                            .withId(11L)
                            .withName("segment-b")
                            .withDescription("second")
                            .withActive(false)
                            .withCreatedAt(LocalDateTime.of(2024, 1, 1, 8, 0))
                            .build()

                    val firstCondition =
                        SegmentConditionFixtures
                            .aUserIdCondition()
                            .withSegmentId(10L)
                            .withConditionValue("100")
                            .withPosition(1)
                            .build()

                    val secondCondition =
                        SegmentConditionFixtures
                            .anEmailCondition()
                            .withSegmentId(11L)
                            .withOperator("CONTAINS")
                            .withConditionValue("\"@okestro.com\"")
                            .withPosition(1)
                            .build()

                    every { segmentRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(firstSegment, secondSegment)
                    every {
                        segmentConditionRepository.findBySegmentIdInOrderBySegmentIdAscPositionAsc(
                            listOf(10L, 11L),
                        )
                    } returns flowOf(firstCondition, secondCondition)

                    val result = useCase.execute(BrowseSegmentUseCaseIn(limit = 10))

                    result.segments.size shouldBe 2
                    result.segments[0].id shouldBe 10L
                    result.segments[0].conditions.size shouldBe 1
                    result.segments[1].id shouldBe 11L
                    result.segments[1].conditions[0].field shouldBe "user.email"
                }
            }

            `when`("no segment exists") {
                then("return empty list without querying conditions") {
                    every { segmentRepository.findAllByOrderByCreatedAtDesc() } returns emptyFlow()

                    val result = useCase.execute(BrowseSegmentUseCaseIn(limit = 10))

                    result.segments.size shouldBe 0
                    verify(exactly = 0) { segmentConditionRepository wasNot Called }
                }
            }

            `when`("limit is below minimum") {
                then("clamps to 1") {
                    val segments =
                        (1..5).map { i ->
                            SegmentFixtures
                                .aSegment()
                                .withId(i.toLong())
                                .withName("seg-$i")
                                .withDescription("desc-$i")
                                .withActive(true)
                                .build()
                        }
                    every { segmentRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(*segments.toTypedArray())
                    every {
                        segmentConditionRepository.findBySegmentIdInOrderBySegmentIdAscPositionAsc(any())
                    } returns emptyFlow()

                    val result = useCase.execute(BrowseSegmentUseCaseIn(limit = 0))

                    result.segments.size shouldBe 1
                }
            }

            `when`("limit is above maximum") {
                then("clamps to 200") {
                    val segments =
                        (1..250).map { i ->
                            SegmentFixtures
                                .aSegment()
                                .withId(i.toLong())
                                .withName("seg-$i")
                                .withDescription("desc-$i")
                                .withActive(true)
                                .build()
                        }
                    every { segmentRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(*segments.toTypedArray())
                    every {
                        segmentConditionRepository.findBySegmentIdInOrderBySegmentIdAscPositionAsc(any())
                    } returns emptyFlow()

                    val result = useCase.execute(BrowseSegmentUseCaseIn(limit = 9999))

                    result.segments.size shouldBe 200
                }
            }
        }
    })
