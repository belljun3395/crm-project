package com.manage.crm.email.application

import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskServicePostEventProcessor
import org.springframework.stereotype.Service

@Service
class CancelNotificationEmailUseCase(
    private val scheduleTaskService: ScheduleTaskServicePostEventProcessor
) {

    suspend fun execute(useCaseIn: CancelNotificationEmailUseCaseIn): CancelNotificationEmailUseCaseOut {
        val eventId = useCaseIn.eventId

        scheduleTaskService.cancel(eventId.value)
        return CancelNotificationEmailUseCaseOut(success = true)
    }
}
