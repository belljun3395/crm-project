package com.manage.crm.email.event.send.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.service.EmailContentService
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.EmailSendHistory
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

// TODO fix duplicate code @see SendNotificationEmailUseCase
@Component
class NotificationEmailSendTimeOutInvokeEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    private val emailSendHistoryRepository: EmailSendHistoryRepository,
    private val userRepository: UserRepository,
    @Qualifier("mailServiceImpl")
    private val mailService: MailService,
    private val emailContentService: EmailContentService,
    private val objectMapper: ObjectMapper
) {
    /**
     * - Find Invoked Event  and Mark it as completed
     * - Send Email to Users
     * - Save Email Send History
     */
    suspend fun handle(event: NotificationEmailSendTimeOutInvokeEvent) {
        val scheduledEvent = (
            scheduledEventRepository
                .findByEventIdAndCompletedFalseForUpdate(event.timeOutEventId)
                ?.complete()
                ?: return
            )
        scheduledEventRepository.save(scheduledEvent)

        val templateId = event.templateId
        val templateVersion = event.templateVersion
        val userIds = event.userIds
        val template = run {
            when {
                (templateVersion != null) -> {
                    emailTemplateHistoryRepository
                        .findByTemplateIdAndVersion(templateId, templateVersion)
                        ?.let {
                            NotificationEmailTemplateVariablesModel(
                                subject = it.subject,
                                body = it.body,
                                variables = it.variables
                            )
                        }
                        ?: throw IllegalArgumentException("Email Template not found by id and version: $templateId, $templateVersion")
                }

                else -> {
                    emailTemplateRepository
                        .findById(templateId)
                        ?.let {
                            NotificationEmailTemplateVariablesModel(
                                subject = it.subject,
                                body = it.body,
                                variables = it.variables
                            )
                        }
                        ?: throw IllegalArgumentException("Email Template not found by id: $templateId")
                }
            }
        }

        val users = userRepository.findAllById(userIds)
        users.collect { user ->
            val email =
                user.userAttributes.getValue(RequiredUserAttributeKey.EMAIL, objectMapper)
            val content = emailContentService.genUserEmailContent(user, template)
            val emailMessageId =
                mailService.send(
                    SendEmailInDto(
                        to = email,
                        subject = template.subject,
                        template = template.body,
                        content = content,
                        emailBody = template.body,
                        destination = email,
                        eventType = SentEmailStatus.SEND
                    )
                ).messageId

            emailSendHistoryRepository.save(
                EmailSendHistory.new(
                    userId = user.id!!,
                    userEmail = email,
                    emailMessageId = emailMessageId,
                    emailBody = template.body,
                    sendStatus = SentEmailStatus.SEND.name
                )
            )
        }
    }
}
