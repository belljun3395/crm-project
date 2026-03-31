package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import com.manage.crm.event.domain.Event
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.event.util.toPercentage
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.application.port.query.SegmentTargetEventReadModel
import com.manage.crm.segment.application.port.query.SegmentTargetUserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import org.springframework.stereotype.Component

/**
 * UC-CAMPAIGN-010
 * Compares campaign conversion metrics across segments.
 *
 * Input: campaign id, segment ids, optional event/time filters.
 * Success: returns segment-wise target/event/conversion metrics.
 */
@Component
class GetCampaignSegmentComparisonUseCase(
    private val campaignEventsService: CampaignEventsService,
    private val segmentReadPort: SegmentReadPort,
    private val userReadPort: UserReadPort
) {
    suspend fun execute(input: GetCampaignSegmentComparisonUseCaseIn): GetCampaignSegmentComparisonUseCaseOut {
        val segmentIds = input.segmentIds
            .mapNotNull { id -> id.takeIf { it > 0 } }
            .distinct()
        if (segmentIds.isEmpty()) {
            throw IllegalArgumentException("segmentIds is required")
        }

        val events = campaignEventsService.findCampaignEvents(input.campaignId, input.startTime, input.endTime)
            .let { baseEvents ->
                val eventName = input.eventName?.trim()?.takeIf { it.isNotBlank() }
                if (eventName == null) {
                    baseEvents
                } else {
                    baseEvents.filter { it.name == eventName }
                }
            }
        val campaignEvents = campaignEventsService.findAllEventsByCampaignId(input.campaignId)
        val campaignUserIds = campaignEvents.map { it.userId }.distinct()
        val users = if (campaignUserIds.isEmpty()) {
            emptyList()
        } else {
            userReadPort.findAllByIdIn(campaignUserIds).map { user ->
                SegmentTargetUserReadModel(
                    id = user.id,
                    userAttributesJson = user.userAttributesJson,
                    createdAt = user.createdAt
                )
            }
        }
        val eventsByUserId = campaignEvents
            .groupBy { it.userId }
            .mapValues { (_, userEvents) ->
                userEvents.map { event ->
                    SegmentTargetEventReadModel(
                        userId = event.userId,
                        name = event.name,
                        occurredAt = event.createdAt
                    )
                }
            }

        val metrics = segmentIds
            .map { segmentId ->
                getMetrics(
                    segmentId = segmentId,
                    filteredEvents = events,
                    users = users,
                    eventsByUserId = eventsByUserId
                )
            }
            .sortedByDescending { it.conversionRate }

        return GetCampaignSegmentComparisonUseCaseOut(
            campaignId = input.campaignId,
            eventName = input.eventName?.trim()?.takeIf { it.isNotBlank() },
            segmentMetrics = metrics
        )
    }

    /**
     * targetUserCount: 세그먼트 대상 유저 수(모집단)
     * eventUserCount: 대상 유저 중 선택 이벤트를 1회 이상 발생한 유저 수(유니크)
     * eventCount: 대상 유저에게서 발생한 이벤트 총 횟수(중복 포함)
     * conversionRate: eventUserCount / targetUserCount * 100 (타겟 중 이벤트 수행 유저 비율)
     */
    private suspend fun getMetrics(
        segmentId: Long,
        filteredEvents: List<Event>,
        users: List<SegmentTargetUserReadModel>,
        eventsByUserId: Map<Long, List<SegmentTargetEventReadModel>>
    ): SegmentComparisonMetricDto {
        val segmentName = segmentReadPort.findNameById(segmentId)
        val segmentTargetUserIds = segmentReadPort.findTargetUserIds(
            segmentId = segmentId,
            users = users,
            eventsByUserId = eventsByUserId
        ).toSet()
        val eventsFromSegmentTargets = filteredEvents.filter { event -> segmentTargetUserIds.contains(event.userId) }
        val usersWithEventCount = eventsFromSegmentTargets.map { it.userId }.toSet().size
        val segmentTargetUserCount = segmentTargetUserIds.size

        return SegmentComparisonMetricDto(
            segmentId = segmentId,
            segmentName = segmentName,
            targetUserCount = segmentTargetUserCount,
            eventUserCount = usersWithEventCount,
            eventCount = eventsFromSegmentTargets.size,
            conversionRate = toPercentage(usersWithEventCount, segmentTargetUserCount)
        )
    }
}
