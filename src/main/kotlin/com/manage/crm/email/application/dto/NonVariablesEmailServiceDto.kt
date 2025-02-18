package com.manage.crm.email.application.dto

import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.infrastructure.mail.SendMailArgs

class NonContent

data class SendEmailArgs(
    override val to: String,
    override val subject: String,
    override val template: String,
    override val content: NonContent,
    override val properties: String = ""
) : SendMailArgs<NonContent, String>

data class SendEmailDto(
    val to: String,
    val subject: String,
    val template: String,
    val content: NonContent,
    val properties: String = "",
    val emailBody: String,
    val destination: String,
    val eventType: SentEmailStatus
) {
    val emailArgs = SendEmailArgs(to, subject, template, content, properties)
}

class NonVariablesEmailServiceDto
