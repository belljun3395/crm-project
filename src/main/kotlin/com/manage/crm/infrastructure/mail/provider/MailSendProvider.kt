package com.manage.crm.infrastructure.mail.provider

interface MailSendProvider {
    fun sendEmail(
        from: String,
        to: String,
        subject: String,
        message: String
    ): String
}
