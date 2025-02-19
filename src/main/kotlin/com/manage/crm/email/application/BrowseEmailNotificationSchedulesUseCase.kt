package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.EmailNotificationScheduleDto
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.support.out
import org.springframework.stereotype.Service

/**
 * - `scheduledTasksView`: `ScheduledTask`를 조회한 결과
 *     - `AWS`의 `EventBridge`에 등록한 스케쥴 정보를 조회
 */
@Service
class BrowseEmailNotificationSchedulesUseCase(
    private val scheduleTaskService: ScheduleTaskService
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
