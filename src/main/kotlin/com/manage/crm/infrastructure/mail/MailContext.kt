package com.manage.crm.infrastructure.mail

import org.thymeleaf.context.Context

class MailContext(
    val context: Context = Context()
) {
    fun setVariable(name: String, value: Any) {
        context.setVariable(name, value)
    }
}
