package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.ScheduleTaskView

interface ScheduleTaskBrowseService {
    suspend fun browseScheduledTasksView(): List<ScheduleTaskView>
}
