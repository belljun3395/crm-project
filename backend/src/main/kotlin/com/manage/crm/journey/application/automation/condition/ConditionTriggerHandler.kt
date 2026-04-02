package com.manage.crm.journey.application.automation.condition

import com.manage.crm.event.domain.Event
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList

class ConditionTriggerHandler(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val conditionExpressionResolver: ConditionExpressionResolver,
    private val conditionEvaluator: ConditionEvaluator,
) {
    private val log = KotlinLogging.logger {}

    suspend fun processConditionTriggeredJourneys(
        event: Event,
        executeJourney: suspend (journey: Journey, event: Event, triggerKey: String) -> Unit,
    ) {
        val conditionJourneys =
            journeyRepository
                .findAllByTriggerTypeAndActiveTrue(JourneyTriggerType.CONDITION.name)
                .toList()
        if (conditionJourneys.isEmpty()) {
            return
        }

        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        conditionJourneys.forEach { journey ->
            runCatching {
                val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
                val conditionExpression =
                    if (!journey.triggerEventName.isNullOrBlank()) {
                        journey.triggerEventName
                    } else {
                        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
                        conditionExpressionResolver.resolve(journey, steps)
                    }

                if (conditionExpression.isNullOrBlank()) {
                    log.warn { "Skip CONDITION journey without condition expression: journeyId=$journeyId" }
                    return@runCatching
                }

                if (!conditionEvaluator.evaluate(conditionExpression, event)) {
                    return@runCatching
                }

                val triggerKey = "$journeyId:CONDITION:$eventId:${event.userId}"
                executeJourney(journey, event, triggerKey)
            }.onFailure { error ->
                val journeyId = journey.id
                log.error(error) { "Failed to process CONDITION journey: journeyId=$journeyId" }
            }
        }
    }
}
