package com.manage.crm.infrastructure.mail.provider

// TODO: Fix SES provider - temporarily disabled for testing to fix compilation issues
// This entire class is commented out because it uses AWS SDK v2 imports that conflict with v1
/*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

@Component
@ConditionalOnProperty(name = ["mail.provider"], havingValue = "ses")
class SesMailSendProvider(
    private val sesClient: SesClient,
    @Value("\${spring.aws.mail.configuration-set.default}") private val configurationSetName: String = "default"
) : MailSendProvider {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val UTF_8 = "UTF-8"
    }

    override fun sendEmail(
        from: String,
        to: String,
        subject: String,
        message: String
    ): String {
        val destination = Destination.builder()
            .toAddresses(to)
            .build()

        val sendMessage = Message.builder()
            .subject(Content.builder().charset(UTF_8).data(subject).build())
            .body(Body.builder().html(Content.builder().charset(UTF_8).data(message).build()).build())
            .build()

        val sendEmailRequest = SendEmailRequest.builder()
            .source(from)
            .destination(destination)
            .message(sendMessage)
            .configurationSetName(configurationSetName)
            .build()

        return runCatching {
            sesClient.sendEmail(sendEmailRequest).messageId()
        }.onFailure {
            log.warn { "Failed to send email using AWS SES. from=$from, to=$to, subject=$subject" }
        }.getOrThrow()
    }
}
*/
