package com.manage.crm.email.application

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.support.out
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PostEmailNotificationSchedulesUseCase(
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskAllService
) {

    suspend fun execute(useCaseIn: PostEmailNotificationSchedulesUseCaseIn): PostEmailNotificationSchedulesUseCaseOut {
        val campaignId = useCaseIn.campaignId
        val templateId = useCaseIn.templateId
        val templateVersion = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds
        val expiredTime = useCaseIn.expiredTime

        val eventId = EventId()
        val newSchedule = scheduleTaskService.newSchedule(
            NotificationEmailSendTimeOutEventInput(
                campaignId,
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds,
                eventId = eventId,
                expiredTime = expiredTime
            )
        )

        return out {
            PostEmailNotificationSchedulesUseCaseOut(newSchedule)
        }
    }
}
