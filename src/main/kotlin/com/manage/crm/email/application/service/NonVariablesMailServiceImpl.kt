package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.SendEmailArgs
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.infrastructure.mail.MailContext
import com.manage.crm.infrastructure.mail.MailSender
import com.manage.crm.infrastructure.mail.MailTemplateProcessor
import com.manage.crm.infrastructure.mail.MailTemplateType
import com.manage.crm.infrastructure.mail.provider.MailSendProvider
import com.manage.crm.user.domain.repository.UserRepository
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.stereotype.Component

@Component
class NonVariablesMailServiceImpl(
    private val userRepository: UserRepository,
    private val mailTemplateProcessor: MailTemplateProcessor,
    mailProperties: MailProperties,
    mailSendProvider: MailSendProvider
) : MailSender<SendEmailArgs>(mailProperties, mailSendProvider), NonVariablesMailService {

    override fun getHtml(args: SendEmailArgs): String {
        val context = MailContext()
        return mailTemplateProcessor.process(args.template, context, MailTemplateType.STRING)
    }

    override suspend fun send(args: SendEmailInDto): SendEmailOutDto {
        val emailArgs = args.emailArgs
        return send(emailArgs).let {
            val userId = userRepository.findByEmail(args.to)?.id
                ?: throw IllegalArgumentException("User not found by email: ${args.to}")
            SendEmailOutDto(
                userId = userId,
                emailBody = args.emailBody,
                messageId = it,
                destination = args.destination,
                provider = EmailProviderType.AWS
            )
        }
    }
}
