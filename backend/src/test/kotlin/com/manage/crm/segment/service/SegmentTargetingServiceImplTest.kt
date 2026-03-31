package com.manage.crm.segment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class SegmentTargetingServiceImplTest : BehaviorSpec({
    lateinit var segmentRepository: SegmentRepository
    lateinit var segmentConditionRepository: SegmentConditionRepository
    lateinit var service: SegmentTargetingService

    suspend fun resolve(
        segmentId: Long,
        users: List<SegmentTargetUser>,
        eventsByUserId: Map<Long, List<SegmentTargetEvent>> = emptyMap()
    ): List<Long> {
        val ruleSet = service.loadRuleSet(segmentId) ?: return emptyList()
        return service.resolveUserIds(ruleSet, users, eventsByUserId)
    }

    beforeContainer {
        segmentRepository = mockk()
        segmentConditionRepository = mockk()
        service = SegmentTargetingService(
            segmentRepository = segmentRepository,
            segmentConditionRepository = segmentConditionRepository,
            objectMapper = ObjectMapper().apply { findAndRegisterModules() }
        )
    }

    given("SegmentTargetingService") {
        `when`("segment does not exist") {
            val segmentId = 999L
            coEvery { segmentRepository.findById(segmentId) } returns null

            then("throws not found") {
                shouldThrow<NotFoundByIdException> {
                    service.loadRuleSet(segmentId)
                }
            }
        }

        `when`("segment is inactive") {
            val segmentId = 11L
            coEvery {
                segmentRepository.findById(segmentId)
            } returns Segment.new(id = segmentId, name = "inactive", description = null, active = false)

            then("returns null rule set") {
                service.loadRuleSet(segmentId).shouldBeNull()
            }
        }

        `when`("segment has no conditions") {
            val segmentId = 12L
            coEvery {
                segmentRepository.findById(segmentId)
            } returns Segment.new(id = segmentId, name = "empty", description = null, active = true)
            every {
                segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId)
            } returns flowOf()

            then("returns null rule set") {
                service.loadRuleSet(segmentId).shouldBeNull()
            }
        }

        `when`("user.email EQ condition matches one user") {
            val segmentId = 10L
            val segment = Segment.new(id = segmentId, name = "seg", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.email",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"target@example.com\"",
                position = 1
            )
            val targetUser = SegmentTargetUser(
                id = 1L,
                userAttributesJson = """{"email":"target@example.com"}""",
                createdAt = LocalDateTime.now().minusDays(1)
            )
            val otherUser = SegmentTargetUser(
                id = 2L,
                userAttributesJson = """{"email":"other@example.com"}""",
                createdAt = LocalDateTime.now().minusDays(1)
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("returns only matching user") {
                resolve(segmentId, listOf(targetUser, otherUser)) shouldBe listOf(1L)
            }
        }

        `when`("user.id GT condition filters users by id range") {
            val segmentId = 20L
            val segment = Segment.new(id = segmentId, name = "high-id", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.id",
                operator = "GT",
                valueType = "NUMBER",
                conditionValue = "100",
                position = 1
            )
            val lowIdUser = SegmentTargetUser(
                id = 50L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.now()
            )
            val highIdUser = SegmentTargetUser(
                id = 200L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("returns only users with id greater than 100") {
                resolve(segmentId, listOf(lowIdUser, highIdUser)) shouldBe listOf(200L)
            }
        }

        `when`("event.name EQ condition with user and event conditions combined") {
            val segmentId = 30L
            val segment = Segment.new(id = segmentId, name = "purchasers", description = null, active = true)
            val emailCondition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.email",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"target@example.com\"",
                position = 1
            )
            val eventCondition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "event.name",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"purchase\"",
                position = 2
            )
            val matchingUser = SegmentTargetUser(
                id = 1L,
                userAttributesJson = """{"email":"target@example.com"}""",
                createdAt = LocalDateTime.now()
            )
            val nonMatchingUser = SegmentTargetUser(
                id = 2L,
                userAttributesJson = """{"email":"target@example.com"}""",
                createdAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every {
                segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId)
            } returns flowOf(emailCondition, eventCondition)

            then("returns only users matching all conditions") {
                resolve(
                    segmentId = segmentId,
                    users = listOf(matchingUser, nonMatchingUser),
                    eventsByUserId = mapOf(
                        1L to listOf(
                            SegmentTargetEvent(
                                userId = 1L,
                                name = "purchase",
                                occurredAt = LocalDateTime.now()
                            )
                        ),
                        2L to listOf(
                            SegmentTargetEvent(
                                userId = 2L,
                                name = "view",
                                occurredAt = LocalDateTime.now()
                            )
                        )
                    )
                ) shouldBe listOf(1L)
            }
        }

        `when`("event.name NEQ condition is evaluated against all user events") {
            val segmentId = 42L
            val segment = Segment.new(id = segmentId, name = "non-purchase-users", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "event.name",
                operator = "NEQ",
                valueType = "STRING",
                conditionValue = "\"purchase\"",
                position = 1
            )
            val firstUser = SegmentTargetUser(
                id = 1L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.now().minusDays(1)
            )
            val secondUser = SegmentTargetUser(
                id = 2L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.now().minusDays(1)
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("keeps only users whose every event name differs from expected value") {
                resolve(
                    segmentId = segmentId,
                    users = listOf(firstUser, secondUser),
                    eventsByUserId = mapOf(
                        1L to listOf(
                            SegmentTargetEvent(
                                userId = 1L,
                                name = "view",
                                occurredAt = LocalDateTime.now()
                            ),
                            SegmentTargetEvent(
                                userId = 1L,
                                name = "click",
                                occurredAt = LocalDateTime.now()
                            )
                        ),
                        2L to listOf(
                            SegmentTargetEvent(
                                userId = 2L,
                                name = "purchase",
                                occurredAt = LocalDateTime.now()
                            )
                        )
                    )
                ) shouldBe listOf(1L)
            }
        }

        `when`("event condition exists but no event input is provided") {
            val segmentId = 45L
            val segment = Segment.new(id = segmentId, name = "event-only", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "event.name",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"purchase\"",
                position = 1
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("returns empty list") {
                resolve(
                    segmentId = segmentId,
                    users = listOf(
                        SegmentTargetUser(
                            id = 1L,
                            userAttributesJson = "{}",
                            createdAt = LocalDateTime.now()
                        )
                    )
                ) shouldBe emptyList()
            }
        }

        `when`("user.createdAt BETWEEN condition is provided") {
            val segmentId = 43L
            val segment = Segment.new(id = segmentId, name = "recent-users", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.createdAt",
                operator = "BETWEEN",
                valueType = "DATETIME",
                conditionValue = "[\"2025-01-01T00:00:00\",\"2025-01-31T23:59:59\"]",
                position = 1
            )
            val inRangeUser = SegmentTargetUser(
                id = 1L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.of(2025, 1, 20, 0, 0)
            )
            val outRangeUser = SegmentTargetUser(
                id = 2L,
                userAttributesJson = "{}",
                createdAt = LocalDateTime.of(2025, 2, 1, 0, 0)
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("returns only users within the datetime range") {
                resolve(segmentId, listOf(inRangeUser, outRangeUser)) shouldBe listOf(1L)
            }
        }

        `when`("condition value is malformed json") {
            val segmentId = 44L
            val segment = Segment.new(id = segmentId, name = "malformed-condition", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.email",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "not-valid-json",
                position = 1
            )
            val user = SegmentTargetUser(
                id = 1L,
                userAttributesJson = """{"email":"target@example.com"}""",
                createdAt = LocalDateTime.now().minusDays(1)
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)

            then("returns empty list instead of throwing") {
                resolve(segmentId, listOf(user)) shouldBe emptyList()
            }
        }
    }
})
