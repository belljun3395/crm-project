package com.manage.crm.segment.adapter.query

import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.application.port.query.SegmentTargetEventReadModel
import com.manage.crm.segment.application.port.query.SegmentTargetUserReadModel
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetEvent
import com.manage.crm.segment.service.SegmentTargetUser
import com.manage.crm.segment.service.SegmentTargetingService
import org.springframework.stereotype.Component

@Component
class SegmentReadAdapter(
    private val segmentRepository: SegmentRepository,
    private val segmentTargetingService: SegmentTargetingService,
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
