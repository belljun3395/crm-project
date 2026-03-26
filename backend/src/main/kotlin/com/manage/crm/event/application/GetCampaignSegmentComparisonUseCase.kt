package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Component
class GetCampaignSegmentComparisonUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventRepository: EventRepository,
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

        val events = findCampaignEvents(input.campaignId, input.startTime, input.endTime)
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

    private suspend fun findCampaignEvents(
        campaignId: Long,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): List<Event> {
        campaignRepository.findById(campaignId) ?: throw NotFoundByIdException("Campaign", campaignId)

        val campaignEventRows = when {
            startTime != null && endTime != null -> campaignEventsRepository.findAllByCampaignIdAndTimeRange(campaignId, startTime, endTime)
            else -> campaignEventsRepository.findAllByCampaignId(campaignId)
        }
        if (campaignEventRows.isEmpty()) {
            return emptyList()
        }

        val eventIds = campaignEventRows.map { it.eventId }.distinct()
        val events = eventRepository.findAllByIdIn(eventIds)
        return events.filter { event ->
            val createdAt = event.createdAt
            if ((startTime != null || endTime != null) && createdAt == null) {
                return@filter false
            }
            val startInclusive = startTime?.let { createdAt == null || createdAt >= it } ?: true
            val endExclusive = endTime?.let { createdAt == null || createdAt < it } ?: true
            startInclusive && endExclusive
        }
    }

    private fun percent(numerator: Int, denominator: Int): Double {
        if (denominator <= 0) {
            return 0.0
        }
        return ((numerator.toDouble() / denominator.toDouble()) * 10000.0).roundToInt() / 100.0
    }
}
