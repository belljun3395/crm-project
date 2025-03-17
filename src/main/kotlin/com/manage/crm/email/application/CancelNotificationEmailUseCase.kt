package com.manage.crm.email.application

import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class CancelNotificationEmailUseCase(
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskService
) {

    suspend fun execute(useCaseIn: CancelNotificationEmailUseCaseIn): CancelNotificationEmailUseCaseOut {
        val eventId = useCaseIn.eventId

        scheduleTaskService.cancel(eventId.value)
        return CancelNotificationEmailUseCaseOut(success = true)
    }
}
