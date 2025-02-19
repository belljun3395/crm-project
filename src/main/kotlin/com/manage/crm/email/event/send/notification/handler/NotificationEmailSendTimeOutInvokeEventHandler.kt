package com.manage.crm.email.event.send.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailArgs
import com.manage.crm.email.application.service.NonVariablesMailService
import com.manage.crm.email.domain.EmailSendHistory
import com.manage.crm.email.domain.model.NotificationEmailTemplatePropertiesModel
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.support.transactional.TransactionTemplates
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class NotificationEmailSendTimeOutInvokeEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    private val emailSendHistoryRepository: EmailSendHistoryRepository,
    private val userRepository: UserRepository,
    private val nonVariablesEmailService: NonVariablesMailService,
    private val objectMapper: ObjectMapper,
    private val transactionalTemplates: TransactionTemplates
) {
    /**
     * - Find Invoked Event  and Mark it as completed
     * - Send Email to Users
     * - Save Email Send History
     */
    suspend fun handle(event: NotificationEmailSendTimeOutInvokeEvent) {
        transactionalTemplates.writer.executeAndAwait {
            val scheduledEvent = (
                scheduledEventRepository
                    .findByEventIdAndCompletedFalseForUpdate(event.timeOutEventId)
                    ?.complete()
                    ?: return@executeAndAwait
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
                                NotificationEmailTemplatePropertiesModel(
                                    subject = it.subject!!,
                                    body = it.body!!
                                )
                            }
                            ?: throw IllegalArgumentException("Email Template not found by id and version: $templateId, $templateVersion")
                    }

                    else -> {
                        emailTemplateRepository
                            .findById(templateId)
                            ?.let {
                                NotificationEmailTemplatePropertiesModel(
                                    subject = it.subject!!,
                                    body = it.body!!
                                )
                            }
                            ?: throw IllegalArgumentException("Email Template not found by id: $templateId")
                    }
                }
            }

            val users = userRepository.findAllById(userIds)
            users.collect { user ->
                val email = user.userAttributes?.getValue(RequiredUserAttributeKey.EMAIL, objectMapper)!!
                val emailMessageId =
                    nonVariablesEmailService.send(
                        SendEmailArgs(
                            to = email,
                            subject = template.subject,
                            template = template.body,
                            content = NonContent()
                        )
                    )

                emailSendHistoryRepository.save(
                    EmailSendHistory(
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
}
