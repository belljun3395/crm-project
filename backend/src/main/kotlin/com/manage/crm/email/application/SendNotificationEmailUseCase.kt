package com.manage.crm.email.application

import arrow.fx.coroutines.parMap
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseOut
import com.manage.crm.email.application.service.EmailContentService
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Email
import com.manage.crm.email.domain.vo.NotificationType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SendNotificationEmailUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    @Qualifier("mailServicePostEventProcessor")
    private val mailService: MailService,
    private val emailContentService: EmailContentService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    val log = KotlinLogging.logger { }

    suspend fun execute(useCaseIn: SendNotificationEmailUseCaseIn): SendNotificationEmailUseCaseOut {
        val templateId = useCaseIn.templateId
        val templateVersion: Float? = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds

        val notificationEmailType = NotificationType.EMAIL.name.lowercase()
        val notificationVariables = getEmailNotificationVariables(templateVersion, templateId)

        val targetUsers = getTargetUsers(userIds, notificationEmailType)
            .mapNotNull { user -> extractEmailAndUser(user, notificationEmailType) }
            .toMap()

        generateNotificationDto(targetUsers, notificationVariables)
            .parMap(Dispatchers.IO, concurrency = 10) {
                mailService.send(it)
            }

        return out {
            SendNotificationEmailUseCaseOut(
                isSuccess = true
            )
        }
    }

    private suspend fun getEmailNotificationVariables(
        templateVersion: Float?,
        templateId: Long
    ): NotificationEmailTemplateVariablesModel {
        return when {
            templateVersion != null -> {
                emailTemplateHistoryRepository
                    .findByTemplateIdAndVersion(templateId, templateVersion)
                    ?.let {
                        NotificationEmailTemplateVariablesModel(
                            subject = it.subject,
                            body = it.body,
                            variables = it.variables
                        )
                    } ?: throw NotFoundByException("EmailTemplate", "templateId", templateId, "version", templateVersion)
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
                    ?: throw NotFoundByIdException("EmailTemplate", templateId)
            }
        }
    }

    private suspend fun getTargetUsers(userIds: List<Long>, sendType: String): List<User> {
        return when {
            userIds.isEmpty() -> {
                userRepository
                    .findAllExistByUserAttributesKey(sendType)
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

    private fun extractEmailAndUser(user: User, notificationEmailType: String): Pair<Email, User>? {
        val attributesMap = parseUserAttributes(user) ?: return null
        val emailValue = attributesMap[notificationEmailType] as? String ?: return null
        return Email(emailValue) to user
    }

    private fun parseUserAttributes(user: User): Map<String, Any>? {
        return try {
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            objectMapper.readValue(user.userAttributes.value, typeRef)
        } catch (e: Exception) {
            log.error(e) { "Failed to parse user attributes for userId: ${user.id}" }
            null
        }
    }

    private fun generateNotificationDto(targetUsers: Map<Email, User>, notificationVariables: NotificationEmailTemplateVariablesModel): List<SendEmailInDto> {
        val emailContentPairList = mutableListOf<Pair<Email, Content>>()
        targetUsers.forEach { (email, user) ->
            val content = emailContentService.genUserEmailContent(user, notificationVariables)
            emailContentPairList.add(email to content)
        }
        return emailContentPairList.map { (email, content) ->
            doMapToNotificationDto(email, content, notificationVariables)
        }
    }

    private fun doMapToNotificationDto(email: Email, content: Content, notificationProperties: NotificationEmailTemplateVariablesModel) = SendEmailInDto(
        to = email.value,
        subject = notificationProperties.subject,
        template = notificationProperties.body,
        content = content,
        emailBody = notificationProperties.body,
        destination = email.value,
        eventType = SentEmailStatus.SEND
    )
}
