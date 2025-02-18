package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.SendEmailArgs
import com.manage.crm.email.application.dto.SendEmailDto
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.infrastructure.mail.MailContext
import com.manage.crm.infrastructure.mail.MailSender
import com.manage.crm.infrastructure.mail.MailTemplateProcessor
import com.manage.crm.infrastructure.mail.MailTemplateType
import com.manage.crm.infrastructure.mail.provider.MailSendProvider
import com.manage.crm.user.domain.repository.UserRepository
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class NonVariablesMailService(
    private val userRepository: UserRepository,
    private val mailTemplateProcessor: MailTemplateProcessor,
    private val eventPublisher: ApplicationEventPublisher,
    mailProperties: MailProperties,
    mailSendProvider: MailSendProvider
) : MailSender<SendEmailArgs>(mailProperties, mailSendProvider) {

    override fun getHtml(args: SendEmailArgs): String {
        val context = MailContext()
        return mailTemplateProcessor.process(args.template, context, MailTemplateType.STRING)
    }

    suspend fun send(args: SendEmailDto) {
        val emailArgs = args.emailArgs
        send(emailArgs).let {
            val userId = userRepository.findByEmail(args.to)?.id ?: throw IllegalArgumentException("User not found by email: ${args.to}")
            eventPublisher.publishEvent(
                EmailSentEvent(
                    userId = userId,
                    emailBody = args.emailBody,
                    messageId = it,
                    destination = args.destination,
                    provider = EmailProviderType.AWS
                )
            )
        }
    }
}
