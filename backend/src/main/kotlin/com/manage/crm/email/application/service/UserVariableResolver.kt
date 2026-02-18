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
 * The resolved map uses the legacy key format (`user_email`) because Thymeleaf's
 * Standard Dialect interprets `${user.email}` as dot-notation property access
 * (i.e., the `email` property of a context variable named `user`), not as a
 * flat variable with a literal dot in its name. Supporting `${user.email}` in
 * HTML templates requires restructuring the MailContext to hold nested objects,
 * which is deferred to a future phase.
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
        return mapOf(variable.legacyKeyWithType() to value)
    }
}
