package com.manage.crm.email.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseOut
import com.manage.crm.email.application.service.NonVariablesMailService
import com.manage.crm.email.domain.model.NotificationEmailTemplatePropertiesModel
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.NotificationType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.support.out
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * - `targetUsers`: 요청 받은 이메일 발송 대상 사용자 목록
 *     - 이메일 주소를 키로 사용하여 사용자를 그룹화
 */
@Service
class SendNotificationEmailUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    @Qualifier("nonVariablesMailServicePostEventProcessor")
    private val nonVariablesEmailService: NonVariablesMailService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    suspend fun execute(useCaseIn: SendNotificationEmailUseCaseIn): SendNotificationEmailUseCaseOut {
        val templateId = useCaseIn.templateId
        val templateVersion: Float? = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds

        val notificationType = NotificationType.EMAIL.name.lowercase()
        val notificationProperties = getEmailNotificationProperties(templateVersion, templateId)

        val targetUsers =
            getTargetUsers(userIds, notificationType)
                .groupBy {
                    objectMapper.readValue(
                        it.userAttributes?.value,
                        Map::class.java
                    )[notificationType] as String
                }

        // TODO: Send email asynchronously
        targetUsers.keys.forEach { email ->
            nonVariablesEmailService.send(
                SendEmailInDto(
                    to = email,
                    subject = notificationProperties.subject,
                    template = notificationProperties.body,
                    content = NonContent(),
                    emailBody = notificationProperties.body,
                    destination = email,
                    eventType = SentEmailStatus.SEND
                )
            )
        }

        return out {
            SendNotificationEmailUseCaseOut(
                isSuccess = true
            )
        }
    }

    private suspend fun getEmailNotificationProperties(
        templateVersion: Float?,
        templateId: Long
    ): NotificationEmailTemplatePropertiesModel {
        return when {
            templateVersion != null -> {
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

    private suspend fun getTargetUsers(userIds: List<Long>, sendType: String): List<User> {
        return when {
            userIds.isEmpty() -> {
                userRepository
                    .findAllExistByUserAttributesKey("email")
            }

            else -> {
                userRepository
                    .findAllByIdIn(userIds)
                    .filter {
                        objectMapper.readValue(
                            it.userAttributes?.value,
                            Map::class.java
                        )[sendType] != null
                    }
            }
        }
    }
}
