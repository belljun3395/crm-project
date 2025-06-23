package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.EmailNotificationScheduleDto
import com.manage.crm.email.application.service.ScheduleTaskQueryService
import com.manage.crm.support.out
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class BrowseEmailNotificationSchedulesUseCase(
    @Qualifier("scheduleTaskServiceImpl")
    private val scheduleTaskService: ScheduleTaskQueryService
) {

    suspend fun execute(): BrowseEmailNotificationSchedulesUseCaseOut {
        val scheduledTasksView = scheduleTaskService.browseScheduledTasksView()

        return out {
            scheduledTasksView.map {
                EmailNotificationScheduleDto(
                    taskName = it.taskName,
                    templateId = it.templateId,
                    userIds = it.userIds,
                    expiredTime = it.expiredTime
                )
            }.let {
                BrowseEmailNotificationSchedulesUseCaseOut(it)
            }
        }
    }
}
