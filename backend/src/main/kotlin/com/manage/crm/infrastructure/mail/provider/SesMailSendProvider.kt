package com.manage.crm.infrastructure.mail.provider

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class SesMailSendProvider(
    private val amazonSimpleEmailService: AmazonSimpleEmailService,
    @Value("\${spring.aws.mail.configuration-set.default}") private val configurationSetName: String
) : MailSendProvider {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val UTF_8 = "utf-8"
    }

    override fun sendEmail(
        from: String,
        to: String,
        subject: String,
        message: String
    ): String {
        val destination = Destination().withToAddresses(to)
        val sendMessage =
            Message()
                .withSubject(Content().withCharset(UTF_8).withData(subject))
                .withBody(Body().withHtml(Content().withCharset(UTF_8).withData(message)))

        val sendEmailRequest =
            SendEmailRequest()
                .withSource(from)
                .withDestination(destination)
                .withMessage(sendMessage)
                .withConfigurationSetName(getWithConfigurationSetName())

        runCatching {
            amazonSimpleEmailService.sendEmail(sendEmailRequest).messageId
        }
            .onFailure {
                log.warn { "Failed to send email using AWS SES. request: $sendEmailRequest" }
            }
            .let {
                return it.getOrThrow()
            }
    }

    fun getWithConfigurationSetName(): String = configurationSetName
}
