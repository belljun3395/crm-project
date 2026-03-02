package com.manage.crm.email.application

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.support.out
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PostEmailNotificationSchedulesUseCase(
    @Qualifier("scheduleTaskServicePostEventProcessor") private val scheduleTaskService: ScheduleTaskAllService,
    private val campaignSegmentsRepository: CampaignSegmentsRepository
) {

    suspend fun execute(useCaseIn: PostEmailNotificationSchedulesUseCaseIn): PostEmailNotificationSchedulesUseCaseOut {
        val campaignId = useCaseIn.campaignId
        val templateId = useCaseIn.templateId
        val templateVersion = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds
        val segmentId = useCaseIn.segmentId
        val expiredTime = useCaseIn.expiredTime

        validateCampaignSegmentLink(campaignId = campaignId, segmentId = segmentId)

        val eventId = EventId()
        val newSchedule = scheduleTaskService.newSchedule(
            NotificationEmailSendTimeOutEventInput(
                campaignId = campaignId,
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds,
                segmentId = segmentId,
                eventId = eventId,
                expiredTime = expiredTime
            )
        )

        return out {
            PostEmailNotificationSchedulesUseCaseOut(newSchedule)
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
}
