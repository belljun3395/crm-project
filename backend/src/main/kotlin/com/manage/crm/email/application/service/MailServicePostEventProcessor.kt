package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.email.support.EmailEventPublisher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class MailServicePostEventProcessor(
    @Qualifier("mailServiceImpl")
    private val mailService: MailService,
    private val emailEventPublisher: EmailEventPublisher
) : MailService {

    override suspend fun send(args: SendEmailInDto): SendEmailOutDto {
        return sendPostEventProcess(mailService.send(args))
    }

    fun sendPostEventProcess(outDto: SendEmailOutDto): SendEmailOutDto {
        emailEventPublisher.publishEvent(
            EmailSentEvent(
                userId = outDto.userId,
                emailBody = outDto.emailBody,
                messageId = outDto.messageId,
                destination = outDto.destination,
                provider = outDto.provider
            )
        )
        return outDto
    }
}
