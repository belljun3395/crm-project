package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.Variable
import com.manage.crm.email.domain.vo.VariableSource
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.user.domain.vo.UserAttributes

/**
 * Strategy interface for resolving a single variable to a map of key-value pairs.
 *
 * The returned map currently uses the legacy-format key only (`user_email`, `campaign_eventCount`)
 * because Thymeleaf interprets dot-notation (`user.email`) as property access rather than a
 * flat variable name. See [UserVariableResolver] for detailed rationale.
 */
interface VariableResolver {
    fun supports(source: VariableSource): Boolean
    fun resolve(variable: Variable, context: VariableResolverContext): Map<String, String>
}

/**
 * Holds the runtime data available during variable resolution.
 */
data class VariableResolverContext(
    val userAttributes: UserAttributes? = null,
    val eventProperties: EventProperties? = null,
    val objectMapper: ObjectMapper? = null
)
