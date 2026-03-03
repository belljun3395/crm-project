package com.manage.crm.event.application

import com.manage.crm.event.application.dto.FunnelStepMetricDto
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.SegmentComparisonMetricDto
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Provides campaign analytics projections for funnel progression and segment comparison.
 */
@Service
class GetCampaignAnalyticsUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventRepository: EventRepository,
    private val segmentRepository: SegmentRepository,
    private val segmentTargetingService: SegmentTargetingService
) {
    suspend fun getFunnel(useCaseIn: GetCampaignFunnelAnalyticsUseCaseIn): GetCampaignFunnelAnalyticsUseCaseOut {
        val steps = useCaseIn.steps.map { it.trim() }.filter { it.isNotBlank() }
        if (steps.size < 2) {
            throw IllegalArgumentException("At least two funnel steps are required")
        }

        val events = findCampaignEvents(useCaseIn.campaignId, useCaseIn.startTime, useCaseIn.endTime)
        val highestReachedStepByUserId = events
            .groupBy { it.userId }
            .mapValues { (_, userEvents) ->
                calculateHighestReachedStepIndex(userEvents, steps)
            }
        var previousQualifiedUserCount = 0

        val metrics = steps.mapIndexed { index, step ->
            val stepEvents = events.filter { it.name == step }
            val qualifiedUserCount = highestReachedStepByUserId
                .values
                .count { reachedIndex -> reachedIndex >= index }

            val conversionFromPrevious = if (index == 0) {
                if (qualifiedUserCount > 0) 100.0 else 0.0
            } else {
                percent(qualifiedUserCount, previousQualifiedUserCount)
            }

            previousQualifiedUserCount = qualifiedUserCount

            FunnelStepMetricDto(
                step = step,
                eventCount = stepEvents.size,
                qualifiedUserCount = qualifiedUserCount,
                conversionFromPrevious = conversionFromPrevious
            )
        }

        return GetCampaignFunnelAnalyticsUseCaseOut(
            campaignId = useCaseIn.campaignId,
            stepMetrics = metrics
        )
    }

    suspend fun compareSegments(useCaseIn: GetCampaignSegmentComparisonUseCaseIn): GetCampaignSegmentComparisonUseCaseOut {
        val segmentIds = useCaseIn.segmentIds
            .mapNotNull { id -> id.takeIf { it > 0 } }
            .distinct()
        if (segmentIds.isEmpty()) {
            throw IllegalArgumentException("segmentIds is required")
        }

        val events = findCampaignEvents(useCaseIn.campaignId, useCaseIn.startTime, useCaseIn.endTime)
            .let { baseEvents ->
                val eventName = useCaseIn.eventName?.trim()?.takeIf { it.isNotBlank() }
                if (eventName == null) {
                    baseEvents
                } else {
                    baseEvents.filter { it.name == eventName }
                }
            }

        val metrics = segmentIds.map { segmentId ->
            val segmentName = segmentRepository.findById(segmentId)?.name
            val targetUserIds = segmentTargetingService.resolveUserIds(segmentId, useCaseIn.campaignId).toSet()
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
            campaignId = useCaseIn.campaignId,
            eventName = useCaseIn.eventName?.trim()?.takeIf { it.isNotBlank() },
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

    private fun calculateHighestReachedStepIndex(events: List<Event>, steps: List<String>): Int {
        if (events.isEmpty()) {
            return -1
        }

        val orderedEvents = events.sortedWith(
            compareBy<Event> { it.createdAt ?: LocalDateTime.MIN }
                .thenBy { it.id ?: Long.MIN_VALUE }
        )
        var reachedStepIndex = -1

        orderedEvents.forEach { event ->
            val nextStepIndex = reachedStepIndex + 1
            if (nextStepIndex < steps.size && event.name == steps[nextStepIndex]) {
                reachedStepIndex = nextStepIndex
            }
        }

        return reachedStepIndex
    }
}
