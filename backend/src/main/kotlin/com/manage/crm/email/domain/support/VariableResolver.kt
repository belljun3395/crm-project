package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.Variable
import com.manage.crm.email.domain.vo.VariableSource
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.user.domain.vo.UserAttributes

/**
 * Strategy interface for resolving a single variable to a map of key-value pairs.
 *
 * The returned map contains both the new-format key (`user.email`) and the legacy-format key
 * (`user_email`) so that HTML templates using either format are supported.
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
