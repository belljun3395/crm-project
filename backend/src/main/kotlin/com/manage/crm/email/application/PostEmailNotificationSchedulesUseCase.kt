package com.manage.crm.email.application

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.support.out
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * - `newSchedule`: 새로 등록한 스케쥴 정보
 */
@Service
class PostEmailNotificationSchedulesUseCase(
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskService
) {

    suspend fun execute(useCaseIn: PostEmailNotificationSchedulesUseCaseIn): PostEmailNotificationSchedulesUseCaseOut {
        val templateId = useCaseIn.templateId
        val templateVersion = useCaseIn.templateVersion
        val userIds = useCaseIn.userIds
        val expiredTime = useCaseIn.expiredTime

        val eventId = EventId()
        val newSchedule = scheduleTaskService.newSchedule(
            NotificationEmailSendTimeOutEventInput(
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
