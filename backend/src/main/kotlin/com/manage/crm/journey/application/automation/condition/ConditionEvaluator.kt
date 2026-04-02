package com.manage.crm.journey.application.automation.condition

import com.manage.crm.event.domain.Event

class ConditionEvaluator {
    fun evaluate(
        conditionExpression: String?,
        event: Event,
    ): Boolean {
        if (conditionExpression.isNullOrBlank()) {
            return true
        }

        val expression = conditionExpression.trim()
        val operator =
            when {
                expression.contains("==") -> "=="
                expression.contains("!=") -> "!="
                else -> throw IllegalArgumentException("Unsupported condition expression: $conditionExpression")
            }

        val parts = expression.split(operator, limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid condition expression: $conditionExpression")
        }

        val left = parts[0].trim()
        val right = parts[1].trim().trim('"').trim('\'')

        if (!left.startsWith("event.")) {
            throw IllegalArgumentException("Condition left operand must start with event.: $conditionExpression")
        }

        val key = left.removePrefix("event.")
        val actual =
            event.properties.value
                .firstOrNull { it.key == key }
                ?.value

        return when (operator) {
            "==" -> actual == right
            "!=" -> actual != right
            else -> false
        }
    }
}
