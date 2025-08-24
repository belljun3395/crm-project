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
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
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
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventsRepository: EventRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    val log = KotlinLogging.logger { }

    suspend fun execute(useCaseIn: SendNotificationEmailUseCaseIn): SendNotificationEmailUseCaseOut {
        val campaignId: Long? = useCaseIn.campaignId
        val templateId = useCaseIn.templateId
        val templateVersion: Float? = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds

        val campaign = campaignId?.let { cId ->
            campaignRepository.findById(cId)
        }

        val notificationEmailType = NotificationType.EMAIL.name.lowercase()
        val notificationVariables = getEmailNotificationVariables(templateVersion, templateId).apply {
            campaign?.let { camp ->
                if (camp.allMatchPropertyKeys(this.variables.value)) {
                    log.error { "Campaign properties and Email template variables mismatch for campaignId: ${camp.id}, templateId: $templateId" }
                    throw IllegalStateException("Campaign properties and Email template variables mismatch")
                }
            }
        }

        val targetUsers = getTargetUsers(userIds, notificationEmailType, campaign?.id)
            .mapNotNull { user -> extractEmailAndUser(user, notificationEmailType) }
            .toMap()

        if (targetUsers.isEmpty()) {
            log.warn { "No target users found for email notification. CampaignId: $campaignId, templateId: $templateId, userIds: $userIds" }
            return out {
                SendNotificationEmailUseCaseOut(
                    isSuccess = false
                )
            }
        }

        log.info { "Sending notification emails to ${targetUsers.size} users. CampaignId: $campaignId, templateId: $templateId" }
        
        generateNotificationDto(targetUsers, notificationVariables, campaign?.id)
            .parMap(Dispatchers.IO, concurrency = 10) { emailDto ->
                try {
                    mailService.send(emailDto)
                } catch (e: Exception) {
                    log.error(e) { "Failed to send email to ${emailDto.to}. CampaignId: $campaignId, templateId: $templateId" }
                    throw e
                }
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

    private suspend fun getTargetUsers(userIds: List<Long>, sendType: String, campaignId: Long?): List<User> {
        return when {
            userIds.isEmpty() -> {
                userRepository
                    .findAllExistByUserAttributesKey(sendType)
            }

            campaignId != null -> {
                val eventIds = campaignEventsRepository.findAllByCampaignId(campaignId).map { it.eventId }
                val allUserIdsInCampaignSet = eventsRepository.findAllByIdIn(eventIds).map { it.userId }.toSet()
                userIds.filter { allUserIdsInCampaignSet.contains(it) }
                    .let { filteredUserIds ->
                        userRepository.findAllByIdIn(filteredUserIds)
                            .filter {
                                objectMapper.readValue(
                                    it.userAttributes.value,
                                    Map::class.java
                                )[sendType] != null
                            }
                    }
            }

            else -> {
                userRepository
                    .findAllByIdIn(userIds)
                    .filter {
                        objectMapper.readValue(
                            it.userAttributes.value,
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
            log.error(e) { "Failed to parse user attributes for userId: ${user.id}. UserAttributes: ${user.userAttributes.value}" }
            null
        }
    }

    private suspend fun generateNotificationDto(targetUsers: Map<Email, User>, notificationVariables: NotificationEmailTemplateVariablesModel, campaignId: Long?): List<SendEmailInDto> {
        return targetUsers.toList().parMap(Dispatchers.IO) { (email, user) ->
            val content = emailContentService.genUserEmailContent(user, notificationVariables, campaignId)
            doMapToNotificationDto(email, content, notificationVariables)
        }
    }

    private fun doMapToNotificationDto(email: Email, content: Content, notificationVariables: NotificationEmailTemplateVariablesModel) = SendEmailInDto(
        to = email.value,
        subject = notificationVariables.subject,
        template = notificationVariables.body,
        content = content,
        emailBody = notificationVariables.body,
        destination = email.value,
        eventType = SentEmailStatus.SEND
    )
}
