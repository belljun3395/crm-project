package com.manage.crm.segment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class SegmentTargetingServiceImplTest : BehaviorSpec({
    lateinit var segmentRepository: SegmentRepository
    lateinit var segmentConditionRepository: SegmentConditionRepository
    lateinit var userRepository: UserRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var eventRepository: EventRepository
    lateinit var service: SegmentTargetingServiceImpl

    beforeContainer {
        segmentRepository = mockk()
        segmentConditionRepository = mockk()
        userRepository = mockk()
        campaignEventsRepository = mockk()
        eventRepository = mockk()
        service = SegmentTargetingServiceImpl(
            segmentRepository = segmentRepository,
            segmentConditionRepository = segmentConditionRepository,
            userRepository = userRepository,
            campaignEventsRepository = campaignEventsRepository,
            eventRepository = eventRepository,
            objectMapper = ObjectMapper().apply { findAndRegisterModules() }
        )
    }

    given("SegmentTargetingServiceImpl resolveUserIds") {
        `when`("segment does not exist") {
            val segmentId = 999L
            coEvery { segmentRepository.findById(segmentId) } returns null

            then("throws not found") {
                shouldThrow<NotFoundByIdException> {
                    service.resolveUserIds(segmentId, null)
                }
            }
        }

        `when`("segment is inactive") {
            val segmentId = 11L
            coEvery {
                segmentRepository.findById(segmentId)
            } returns Segment.new(id = segmentId, name = "inactive", description = null, active = false)

            then("returns empty list") {
                service.resolveUserIds(segmentId, null) shouldBe emptyList()
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

            then("returns empty list") {
                service.resolveUserIds(segmentId, null) shouldBe emptyList()
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
            val targetUser = User.new(
                id = 1L,
                externalId = "target",
                userAttributes = UserAttributes("""{"email":"target@example.com"}"""),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )
            val otherUser = User.new(
                id = 2L,
                externalId = "other",
                userAttributes = UserAttributes("""{"email":"other@example.com"}"""),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            every { userRepository.findAll() } returns flowOf(targetUser, otherUser)

            then("returns only matching user") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(1L)
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
            val lowIdUser = User.new(
                id = 50L,
                externalId = "low",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            val highIdUser = User.new(
                id = 200L,
                externalId = "high",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            every { userRepository.findAll() } returns flowOf(lowIdUser, highIdUser)

            then("returns only users with id greater than 100") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(200L)
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
            val matchingUser = User.new(
                id = 1L,
                externalId = "buyer",
                userAttributes = UserAttributes("""{"email":"target@example.com"}"""),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            val nonMatchingUser = User.new(
                id = 2L,
                externalId = "visitor",
                userAttributes = UserAttributes("""{"email":"target@example.com"}"""),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every {
                segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId)
            } returns flowOf(emailCondition, eventCondition)
            every { userRepository.findAll() } returns flowOf(matchingUser, nonMatchingUser)
            coEvery { eventRepository.findAllByUserIdIn(listOf(1L, 2L)) } returns listOf(
                Event.new(
                    id = 100L,
                    name = "purchase",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                ),
                Event.new(
                    id = 101L,
                    name = "view",
                    userId = 2L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                )
            )

            then("returns only users matching all conditions") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(1L)
            }
        }

        `when`("campaign scope is provided with no events") {
            val segmentId = 40L
            val campaignId = 99L
            val segment = Segment.new(id = segmentId, name = "campaign-seg", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.email",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"x@example.com\"",
                position = 1
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            coEvery { campaignEventsRepository.findEventIdsByCampaignId(campaignId) } returns emptyList()

            then("returns empty list") {
                service.resolveUserIds(segmentId, campaignId) shouldBe emptyList()
            }
        }

        `when`("campaign scope contains duplicated event ids") {
            val segmentId = 41L
            val campaignId = 100L
            val segment = Segment.new(id = segmentId, name = "campaign-users", description = null, active = true)
            val condition = SegmentCondition.new(
                segmentId = segmentId,
                fieldName = "user.email",
                operator = "EQ",
                valueType = "STRING",
                conditionValue = "\"target@example.com\"",
                position = 1
            )
            val targetUser = User.new(
                id = 1L,
                externalId = "target",
                userAttributes = UserAttributes("""{"email":"target@example.com"}"""),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )
            val otherUser = User.new(
                id = 2L,
                externalId = "other",
                userAttributes = UserAttributes("""{"email":"other@example.com"}"""),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            coEvery { campaignEventsRepository.findEventIdsByCampaignId(campaignId) } returns listOf(100L, 100L, 101L)
            coEvery { eventRepository.findAllByIdIn(listOf(100L, 101L)) } returns listOf(
                Event.new(
                    id = 100L,
                    name = "view",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                ),
                Event.new(
                    id = 101L,
                    name = "click",
                    userId = 2L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                )
            )
            coEvery { userRepository.findAllByIdIn(listOf(1L, 2L)) } returns listOf(targetUser, otherUser)

            then("deduplicates event ids and returns matched campaign users") {
                service.resolveUserIds(segmentId, campaignId) shouldBe listOf(1L)
                coVerify(exactly = 1) { eventRepository.findAllByIdIn(listOf(100L, 101L)) }
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
            val firstUser = User.new(
                id = 1L,
                externalId = "u-1",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )
            val secondUser = User.new(
                id = 2L,
                externalId = "u-2",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            every { userRepository.findAll() } returns flowOf(firstUser, secondUser)
            coEvery { eventRepository.findAllByUserIdIn(listOf(1L, 2L)) } returns listOf(
                Event.new(
                    id = 200L,
                    name = "view",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                ),
                Event.new(
                    id = 201L,
                    name = "click",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                ),
                Event.new(
                    id = 202L,
                    name = "purchase",
                    userId = 2L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                )
            )

            then("keeps only users whose every event name differs from expected value") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(1L)
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
            val inRangeUser = User.new(
                id = 1L,
                externalId = "in-range",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.of(2025, 1, 20, 0, 0),
                updatedAt = LocalDateTime.now()
            )
            val outRangeUser = User.new(
                id = 2L,
                externalId = "out-range",
                userAttributes = UserAttributes("{}"),
                createdAt = LocalDateTime.of(2025, 2, 1, 0, 0),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            every { userRepository.findAll() } returns flowOf(inRangeUser, outRangeUser)

            then("returns only users within the datetime range") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(1L)
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
            val user = User.new(
                id = 1L,
                externalId = "u-1",
                userAttributes = UserAttributes("""{"email":"target@example.com"}"""),
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now()
            )

            coEvery { segmentRepository.findById(segmentId) } returns segment
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(condition)
            every { userRepository.findAll() } returns flowOf(user)

            then("returns empty list instead of throwing") {
                service.resolveUserIds(segmentId, null) shouldBe emptyList()
            }
        }
    }
})
