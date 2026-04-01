package com.manage.crm.journey.application.automation.condition

import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep

class ConditionExpressionResolver {
    fun resolve(
        journey: Journey,
        steps: List<JourneyStep>,
    ): String? {
        if (!journey.triggerEventName.isNullOrBlank()) {
            return journey.triggerEventName
        }

        return steps
            .firstOrNull { step ->
                runCatching { JourneyStepType.from(step.stepType) }.getOrNull() == JourneyStepType.BRANCH &&
                    !step.conditionExpression.isNullOrBlank()
            }?.conditionExpression
    }
}
