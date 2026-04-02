package com.manage.crm.segment.adapter.query

import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.application.port.query.SegmentTargetEventReadModel
import com.manage.crm.segment.application.port.query.SegmentTargetUserReadModel
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetEvent
import com.manage.crm.segment.service.SegmentTargetUser
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.user.application.port.query.UserReadPort
import org.springframework.stereotype.Component

@Component
class SegmentReadAdapter(
    private val segmentRepository: SegmentRepository,
    private val segmentTargetingService: SegmentTargetingService,
    private val userReadPort: UserReadPort,
    private val eventReadPort: EventReadPort,
) : SegmentReadPort {
    override suspend fun existsById(segmentId: Long): Boolean = segmentRepository.findById(segmentId) != null

    override suspend fun findNameById(segmentId: Long): String? = segmentRepository.findById(segmentId)?.name

    override suspend fun findTargetUserIds(
        segmentId: Long,
        users: List<SegmentTargetUserReadModel>,
        eventsByUserId: Map<Long, List<SegmentTargetEventReadModel>>,
    ): List<Long> {
        val ruleSet = segmentTargetingService.loadRuleSet(segmentId) ?: return emptyList()
        val targetUsers = users.map { it.toTargetUser() }
        val targetEventsByUserId =
            eventsByUserId.mapValues { (_, events) ->
                events.map { event -> event.toTargetEvent() }
            }
        return segmentTargetingService.resolveUserIds(ruleSet, targetUsers, targetEventsByUserId)
    }

    override suspend fun findTargetUserIds(segmentId: Long): List<Long> {
        val users =
            userReadPort.findAll().map { user ->
                SegmentTargetUserReadModel(
                    id = user.id,
                    userAttributesJson = user.userAttributesJson,
                    createdAt = user.createdAt,
                )
            }
        if (users.isEmpty()) return emptyList()
        val eventsByUserId =
            eventReadPort
                .findAllByUserIdIn(users.map { it.id })
                .groupBy { it.userId }
                .mapValues { (_, events) ->
                    events.map { event ->
                        SegmentTargetEventReadModel(
                            userId = event.userId,
                            name = event.name,
                            occurredAt = event.createdAt,
                        )
                    }
                }
        return findTargetUserIds(segmentId, users, eventsByUserId)
    }
}

private fun SegmentTargetUserReadModel.toTargetUser(): SegmentTargetUser =
    SegmentTargetUser(
        id = id,
        userAttributesJson = userAttributesJson,
        createdAt = createdAt,
    )

private fun SegmentTargetEventReadModel.toTargetEvent(): SegmentTargetEvent =
    SegmentTargetEvent(
        userId = userId,
        name = name,
        occurredAt = occurredAt,
    )
