package com.manage.crm.action.application

object VariableTemplateRenderer {
    private val tokenRegex = Regex("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}")

    fun render(
        template: String,
        variables: Map<String, String>,
    ): String =
        tokenRegex.replace(template) { match ->
            variables[match.groupValues[1]] ?: ""
        }
}
