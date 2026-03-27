package com.manage.crm.event.application

import com.manage.crm.event.application.dto.FunnelStepMetricDto
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseOut
import com.manage.crm.event.util.toPercentage
import com.manage.crm.event.domain.Event
import com.manage.crm.event.service.CampaignEventsService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * UC-CAMPAIGN-009
 * Calculates campaign funnel analytics for ordered event steps.
 *
 * Input: campaign id, funnel steps, optional time range.
 * Success: returns per-step event counts and conversion.
 */
@Component
class GetCampaignFunnelAnalyticsUseCase(
    private val campaignEventsService: CampaignEventsService
) {
    suspend fun execute(input: GetCampaignFunnelAnalyticsUseCaseIn): GetCampaignFunnelAnalyticsUseCaseOut {
        val steps = input.steps.map { it.trim() }.filter { it.isNotBlank() }
        if (steps.size < 2) {
            throw IllegalArgumentException("At least two funnel steps are required")
        }

        val events = campaignEventsService.findCampaignEvents(input.campaignId, input.startTime, input.endTime)
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
                toPercentage(qualifiedUserCount, previousQualifiedUserCount)
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
