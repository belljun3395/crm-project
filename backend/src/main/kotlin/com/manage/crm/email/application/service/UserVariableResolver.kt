package com.manage.crm.email.application.service

import com.manage.crm.email.domain.support.VariableResolver
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.email.domain.vo.Variable
import com.manage.crm.email.domain.vo.VariableSource
import org.springframework.stereotype.Component

/**
 * Resolves USER-source variables from UserAttributes.
 *
 * Resolution policy:
 * - Attribute exists → resolve to its value
 * - Attribute missing → throw IllegalArgumentException (strict)
 *
 * Returns both the new-format key (`user.email`) and the legacy-format key (`user_email`)
 * so that templates using either syntax continue to work.
 */
@Component
class UserVariableResolver : VariableResolver {

    override fun supports(source: VariableSource): Boolean = source == VariableSource.USER

    override fun resolve(variable: Variable, context: VariableResolverContext): Map<String, String> {
        val userAttributes = requireNotNull(context.userAttributes) {
            "UserAttributes is required for UserVariableResolver"
        }
        val objectMapper = requireNotNull(context.objectMapper) {
            "ObjectMapper is required for UserVariableResolver"
        }

        if (!userAttributes.isExist(variable.key, objectMapper)) {
            throw IllegalArgumentException("UserAttribute '${variable.key}' not found for variable ${variable.keyWithSource()}")
        }

        val value = userAttributes.getValue(variable.key, objectMapper)
        return mapOf(
            variable.keyWithSource() to value,
            variable.legacyKeyWithType() to value
        )
    }
}
