package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentUseCaseIn
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class GetSegmentUseCaseTest : BehaviorSpec({
    lateinit var segmentRepository: SegmentRepository
    lateinit var segmentConditionRepository: SegmentConditionRepository
    lateinit var useCase: GetSegmentUseCase

    beforeTest {
        segmentRepository = mockk()
        segmentConditionRepository = mockk()
        useCase = GetSegmentUseCase(
            segmentRepository = segmentRepository,
            segmentConditionRepository = segmentConditionRepository,
            objectMapper = jacksonObjectMapper()
        )
    }

    given("UC-SEGMENT-003 GetSegmentUseCase") {
        `when`("segment does not exist") {
            then("throw not found") {
                val segmentId = 999L
                coEvery { segmentRepository.findById(segmentId) } returns null

                shouldThrow<NotFoundByIdException> {
                    useCase.execute(GetSegmentUseCaseIn(segmentId))
                }
            }
        }

        `when`("segment exists") {
            then("return segment with conditions") {
                val segmentId = 1L
                val segment = Segment.new(
                    id = segmentId,
                    name = "power-users",
                    description = "Power users",
                    active = true
                ).apply {
                    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
                }
                val condition = SegmentCondition.new(
                    segmentId = segmentId,
                    fieldName = "user.id",
                    operator = "GT",
                    valueType = "NUMBER",
                    conditionValue = "100",
                    position = 1
                )

                coEvery { segmentRepository.findById(segmentId) } returns segment
                every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

                val result = useCase.execute(GetSegmentUseCaseIn(segmentId))

                result.segment.id shouldBe segmentId
                result.segment.conditions.size shouldBe 1
                result.segment.conditions[0].field shouldBe "user.id"
                result.segment.conditions[0].value.asInt() shouldBe 100
            }
        }
    }
})
