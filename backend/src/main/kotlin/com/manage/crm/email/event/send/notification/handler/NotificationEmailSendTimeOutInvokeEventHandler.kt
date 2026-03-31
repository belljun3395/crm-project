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
import com.manage.crm.event.application.port.query.CampaignEventReadPort
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.application.port.query.SegmentTargetEventReadModel
import com.manage.crm.segment.application.port.query.SegmentTargetUserReadModel
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import kotlinx.coroutines.flow.toList
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
    private val eventReadPort: EventReadPort,
    private val campaignEventReadPort: CampaignEventReadPort,
    private val segmentReadPort: SegmentReadPort,
    private val campaignSegmentsRepository: CampaignSegmentsRepository,
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
        val campaignId = event.campaignId
        validateCampaignSegmentLink(campaignId = campaignId, segmentId = event.segmentId)
        val (segmentUsers, eventsByUserId) = if (event.segmentId != null) {
            resolveSegmentEvaluationScope(campaignId)
        } else {
            emptyList<SegmentTargetUserReadModel>() to emptyMap()
        }
        val userIds = if (event.segmentId != null) {
            segmentReadPort.findTargetUserIds(
                segmentId = event.segmentId,
                users = segmentUsers,
                eventsByUserId = eventsByUserId
            )
        } else {
            event.userIds
        }
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
            // TODO check if notification need to send to user filter by campaign
            val content = emailContentService.genUserEmailContent(user, template, campaignId) ?: return@collect
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

    private suspend fun validateCampaignSegmentLink(campaignId: Long?, segmentId: Long?) {
        if (campaignId == null || segmentId == null) {
            return
        }

        val linked = campaignSegmentsRepository.existsByCampaignIdAndSegmentId(campaignId, segmentId)
        if (linked) {
            return
        }

        throw IllegalArgumentException("segmentId $segmentId is not linked to campaignId $campaignId")
    }

    private suspend fun resolveSegmentEvaluationScope(
        campaignId: Long?
    ): Pair<List<SegmentTargetUserReadModel>, Map<Long, List<SegmentTargetEventReadModel>>> {
        if (campaignId != null) {
            val campaignEventIds = campaignEventReadPort.findEventIdsByCampaignId(campaignId).distinct()
            if (campaignEventIds.isEmpty()) {
                return emptyList<SegmentTargetUserReadModel>() to emptyMap()
            }

            val campaignEvents = eventReadPort.findAllByIdIn(campaignEventIds)
            val campaignUserIds = campaignEvents.map { it.userId }.distinct()
            if (campaignUserIds.isEmpty()) {
                return emptyList<SegmentTargetUserReadModel>() to emptyMap()
            }

            val users = userRepository.findAllByIdIn(campaignUserIds).map { user ->
                SegmentTargetUserReadModel(
                    id = requireNotNull(user.id) { "User id cannot be null" },
                    userAttributesJson = user.userAttributes.value,
                    createdAt = user.createdAt
                )
            }
            val eventsByUserId = campaignEvents
                .groupBy { it.userId }
                .mapValues { (_, events) ->
                    events.map { event ->
                        SegmentTargetEventReadModel(
                            userId = event.userId,
                            name = event.name,
                            occurredAt = event.createdAt
                        )
                    }
                }
            return users to eventsByUserId
        }

        val users = userRepository.findAll().toList().mapNotNull { user ->
            val userId = user.id ?: return@mapNotNull null
            SegmentTargetUserReadModel(
                id = userId,
                userAttributesJson = user.userAttributes.value,
                createdAt = user.createdAt
            )
        }
        if (users.isEmpty()) {
            return emptyList<SegmentTargetUserReadModel>() to emptyMap()
        }
        val eventsByUserId = eventReadPort.findAllByUserIdIn(users.map { it.id })
            .groupBy { it.userId }
            .mapValues { (_, events) ->
                events.map { event ->
                    SegmentTargetEventReadModel(
                        userId = event.userId,
                        name = event.name,
                        occurredAt = event.createdAt
                    )
                }
            }
        return users to eventsByUserId
    }
}
