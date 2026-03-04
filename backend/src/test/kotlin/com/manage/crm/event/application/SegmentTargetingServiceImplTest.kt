package com.manage.crm.event.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
            objectMapper = ObjectMapper()
        )
    }

    given("resolveUserIds") {
        `when`("segment conditions match user field and event field") {
            val segmentId = 10L
            val segment = Segment.new(id = segmentId, name = "seg", description = null, active = true)
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
            every { segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId) } returns flowOf(emailCondition, eventCondition)
            every { userRepository.findAll() } returns flowOf(targetUser, otherUser)
            coEvery { eventRepository.findAllByUserIdIn(listOf(1L, 2L)) } returns listOf(
                Event.new(
                    id = 100L,
                    name = "purchase",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now().minusHours(1)
                ),
                Event.new(
                    id = 101L,
                    name = "view",
                    userId = 2L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now().minusHours(1)
                )
            )

            then("returns only users matching all conditions") {
                service.resolveUserIds(segmentId, null) shouldBe listOf(1L)
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
    }
})
