package com.manage.crm.infrastructure.mail.provider

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["mail.provider"], havingValue = "javamail")
class JavamailSendProvider(
    private val emailSender: JavaMailSender
) : MailSendProvider {
    companion object {
        private const val UTF_8 = "utf-8"
    }

    override fun sendEmail(
        from: String,
        to: String,
        subject: String,
        message: String
    ): String {
        val sendMessage: MimeMessage = emailSender.createMimeMessage()
        val helper = MimeMessageHelper(sendMessage, UTF_8)
        try {
            helper.setFrom(from)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(message, true)
        } catch (e: MessagingException) {
            throw RuntimeException(e)
        }
        emailSender.send(sendMessage)
        return sendMessage.messageID
    }
}
