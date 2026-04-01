package com.manage.crm.infrastructure.mail.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["mail.provider"], havingValue = "mock")
class MockMailSendProvider : MailSendProvider {
    private val log = KotlinLogging.logger {}

    override fun sendEmail(
        from: String,
        to: String,
        subject: String,
        message: String,
    ): String {
        val messageId = "mock-${UUID.randomUUID()}"
        log.debug { "Mock email sent: from=$from, to=$to, subject=$subject, messageId=$messageId" }
        return messageId
    }
}
