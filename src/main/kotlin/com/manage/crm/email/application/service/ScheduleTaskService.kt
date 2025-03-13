package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput

interface ScheduleTaskService {
    fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String
    fun cancel(scheduleName: String)
    fun reSchedule(input: NotificationEmailSendTimeOutEventInput)
}
