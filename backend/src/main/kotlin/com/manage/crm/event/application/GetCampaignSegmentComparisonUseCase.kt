package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import com.manage.crm.event.domain.Event
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.event.util.toPercentage
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetingService
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
    private val segmentRepository: SegmentRepository,
    private val segmentTargetingService: SegmentTargetingService
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

        val metrics = segmentIds
            .map { segmentId -> getMetrics(segmentId, input.campaignId, events) }
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
    private suspend fun getMetrics(segmentId: Long, campaignId: Long, filteredEvents: List<Event>): SegmentComparisonMetricDto {
        val segmentName = segmentRepository.findById(segmentId)?.name
        val segmentTargetUserIds = segmentTargetingService.resolveUserIds(segmentId, campaignId).toSet()
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
