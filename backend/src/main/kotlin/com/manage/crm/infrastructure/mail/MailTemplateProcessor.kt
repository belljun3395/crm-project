package com.manage.crm.infrastructure.mail

import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine

@Component
class MailTemplateProcessor(
    private val templateEngines: Map<String, TemplateEngine>
) {
    fun process(
        template: String,
        context: MailContext,
        templateType: MailTemplateType? = MailTemplateType.HTML
    ): String {
        templateEngines.keys
            .find { it.contains(templateType!!.name, ignoreCase = true) }
            ?.let { return templateEngines[it]!!.process(template, context.context) }
            ?: throw IllegalArgumentException("Invalid template type")
    }
}
