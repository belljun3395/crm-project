package com.manage.crm.journey.application.automation.condition

import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import io.github.oshai.kotlinlogging.KotlinLogging

class ConditionExpressionResolver {
    private val log = KotlinLogging.logger {}

    fun resolve(
        journey: Journey,
        steps: List<JourneyStep>,
    ): String? {
        if (!journey.triggerEventName.isNullOrBlank()) {
            return journey.triggerEventName
        }

        return steps
            .firstOrNull { step ->
                runCatching { JourneyStepType.from(step.stepType) }
                    .onFailure { e -> log.debug(e) { "Failed to parse JourneyStepType from '${step.stepType}'" } }
                    .getOrNull() == JourneyStepType.BRANCH &&
                    !step.conditionExpression.isNullOrBlank()
            }?.conditionExpression
    }
}
