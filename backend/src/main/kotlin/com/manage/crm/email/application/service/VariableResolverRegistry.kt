package com.manage.crm.email.application.service

import com.manage.crm.email.domain.support.VariableResolver
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.email.domain.vo.Variables
import org.springframework.stereotype.Service

/**
 * Routes variable resolution to the appropriate [VariableResolver] based on the variable's source.
 *
 * Adding a new variable source requires only:
 * 1. Adding a new value to [VariableSource]
 * 2. Implementing and registering a new [VariableResolver] Spring component
 *
 * No changes to [EmailContentService] or any core service are needed.
 */
@Service
class VariableResolverRegistry(
    private val resolvers: List<VariableResolver>
) {
    /**
     * Resolves all variables in [variables] and merges the results into a single map.
     */
    fun resolveAll(variables: Variables, context: VariableResolverContext): Map<String, String> {
        return variables.value.flatMap { variable ->
            val resolver = resolvers.find { it.supports(variable.source) }
                ?: throw IllegalArgumentException(
                    "No VariableResolver found for source '${variable.source}'. " +
                        "Register a VariableResolver @Component that supports this source."
                )
            resolver.resolve(variable, context).entries
        }.associate { it.key to it.value }
    }
}
