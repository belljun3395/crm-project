package com.manage.crm.event.application

import com.manage.crm.event.application.dto.FunnelStepMetricDto
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseOut
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Component
class GetCampaignFunnelAnalyticsUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventRepository: EventRepository
) {
    suspend fun execute(input: GetCampaignFunnelAnalyticsUseCaseIn): GetCampaignFunnelAnalyticsUseCaseOut {
        val steps = input.steps.map { it.trim() }.filter { it.isNotBlank() }
        if (steps.size < 2) {
            throw IllegalArgumentException("At least two funnel steps are required")
        }

        val events = findCampaignEvents(input.campaignId, input.startTime, input.endTime)
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
            campaignId = input.campaignId,
            stepMetrics = metrics
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
