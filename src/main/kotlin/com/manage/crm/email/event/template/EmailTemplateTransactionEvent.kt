package com.manage.crm.email.event.template

abstract class EmailTemplateTransactionEvent

abstract class EmailTemplateTransactionAfterCompletionEvent : EmailTemplateTransactionEvent()

class PostEmailTemplateEvent(
    val templateId: Long
) : EmailTemplateTransactionAfterCompletionEvent()
