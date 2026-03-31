package com.manage.crm.segment.adapter.query

import com.manage.crm.event.application.port.query.CampaignEventReadPort
import com.manage.crm.event.application.port.query.EventReadModel
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetEvent
import com.manage.crm.segment.service.SegmentTargetUser
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import org.springframework.stereotype.Component

@Component
class SegmentReadAdapter(
    private val segmentRepository: SegmentRepository,
    private val userReadPort: UserReadPort,
    private val eventReadPort: EventReadPort,
    private val campaignEventReadPort: CampaignEventReadPort,
    private val segmentTargetingService: SegmentTargetingService
) : SegmentReadPort {
    override suspend fun existsById(segmentId: Long): Boolean {
        return segmentRepository.findById(segmentId) != null
    }

    override suspend fun findNameById(segmentId: Long): String? {
        return segmentRepository.findById(segmentId)?.name
    }

    override suspend fun findTargetUserIds(segmentId: Long, campaignId: Long?): List<Long> {
        val ruleSet = segmentTargetingService.loadRuleSet(segmentId) ?: return emptyList()
        val (users, eventsByUserId) = if (campaignId != null) {
            resolveCampaignScope(campaignId, ruleSet.requiresEventCondition)
        } else {
            resolveGlobalScope(ruleSet.requiresEventCondition)
        }
        return segmentTargetingService.resolveUserIds(ruleSet, users, eventsByUserId)
    }

    private suspend fun resolveCampaignScope(
        campaignId: Long,
        requiresEventCondition: Boolean
    ): Pair<List<SegmentTargetUser>, Map<Long, List<SegmentTargetEvent>>> {
        val campaignEventIds = campaignEventReadPort.findEventIdsByCampaignId(campaignId).distinct()
        if (campaignEventIds.isEmpty()) {
            return emptyList<SegmentTargetUser>() to emptyMap()
        }

        val campaignEvents = eventReadPort.findAllByIdIn(campaignEventIds)
        val campaignUserIds = campaignEvents.map { it.userId }.distinct()
        if (campaignUserIds.isEmpty()) {
            return emptyList<SegmentTargetUser>() to emptyMap()
        }

        val users = userReadPort.findAllByIdIn(campaignUserIds).map { it.toTargetUser() }
        val eventsByUserId = if (requiresEventCondition) {
            campaignEvents.groupBy { it.userId }.mapValues { (_, events) -> events.map { it.toTargetEvent() } }
        } else {
            emptyMap()
        }
        return users to eventsByUserId
    }

    private suspend fun resolveGlobalScope(
        requiresEventCondition: Boolean
    ): Pair<List<SegmentTargetUser>, Map<Long, List<SegmentTargetEvent>>> {
        val users = userReadPort.findAll().map { it.toTargetUser() }
        if (users.isEmpty()) {
            return emptyList<SegmentTargetUser>() to emptyMap()
        }

        val eventsByUserId = if (requiresEventCondition) {
            val userIds = users.map { it.id }
            eventReadPort.findAllByUserIdIn(userIds)
                .groupBy { it.userId }
                .mapValues { (_, events) -> events.map { it.toTargetEvent() } }
        } else {
            emptyMap()
        }
        return users to eventsByUserId
    }
}

private fun UserReadModel.toTargetUser(): SegmentTargetUser {
    return SegmentTargetUser(
        id = id,
        userAttributesJson = userAttributesJson,
        createdAt = createdAt
    )
}

private fun EventReadModel.toTargetEvent(): SegmentTargetEvent {
    return SegmentTargetEvent(
        userId = userId,
        name = name,
        occurredAt = createdAt
    )
}
