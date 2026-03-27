package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetingService
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

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

        val metrics = segmentIds.map { segmentId ->
            val segmentName = segmentRepository.findById(segmentId)?.name
            val targetUserIds = segmentTargetingService.resolveUserIds(segmentId, input.campaignId).toSet()
            val segmentEvents = events.filter { event -> targetUserIds.contains(event.userId) }
            val eventUserCount = segmentEvents.map { it.userId }.toSet().size
            val targetUserCount = targetUserIds.size

            SegmentComparisonMetricDto(
                segmentId = segmentId,
                segmentName = segmentName,
                targetUserCount = targetUserCount,
                eventUserCount = eventUserCount,
                eventCount = segmentEvents.size,
                conversionRate = percent(eventUserCount, targetUserCount)
            )
        }.sortedByDescending { it.conversionRate }

        return GetCampaignSegmentComparisonUseCaseOut(
            campaignId = input.campaignId,
            eventName = input.eventName?.trim()?.takeIf { it.isNotBlank() },
            segmentMetrics = metrics
        )
    }

    private fun percent(numerator: Int, denominator: Int): Double {
        if (denominator <= 0) {
            return 0.0
        }
        return ((numerator.toDouble() / denominator.toDouble()) * 10000.0).roundToInt() / 100.0
    }
}
