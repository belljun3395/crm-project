package com.manage.crm.email.application.service

import com.manage.crm.email.domain.support.VariableResolver
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.email.domain.vo.Variable
import com.manage.crm.email.domain.vo.VariableSource
import org.springframework.stereotype.Component

/**
 * Resolves CAMPAIGN-source variables from EventProperties.
 *
 * Resolution policy:
 * - Property key exists → resolve to its value
 * - Property key missing → silently skip (relaxed, no entry added to result map)
 *
 * Returns both the new-format key (`campaign.eventCount`) and the legacy-format key
 * (`campaign_eventCount`) so that templates using either syntax continue to work.
 */
@Component
class CampaignVariableResolver : VariableResolver {

    override fun supports(source: VariableSource): Boolean = source == VariableSource.CAMPAIGN

    override fun resolve(variable: Variable, context: VariableResolverContext): Map<String, String> {
        val eventProperties = context.eventProperties ?: return emptyMap()
        val propertyKeys = eventProperties.getKeys()

        if (!propertyKeys.contains(variable.key)) {
            return emptyMap()
        }

        val value = eventProperties.getValue(variable.key)
        return mapOf(
            variable.keyWithSource() to value,
            variable.legacyKeyWithType() to value
        )
    }
}
