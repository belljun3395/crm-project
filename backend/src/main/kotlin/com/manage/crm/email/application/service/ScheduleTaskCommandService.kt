package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput

/**
 * 스케줄 관련 명령 서비스를 제공하는 인터페이스
 */
interface ScheduleTaskCommandService {
    /**
     * 새로운 스케줄을 등록합니다.
     */
    fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String

    /**
     * 등록한 스케줄을 취소합니다.
     */
    fun cancel(scheduleName: String)

    /**
     * 등록한 스케줄을 재등록합니다.
     */
    fun reSchedule(input: NotificationEmailSendTimeOutEventInput)
}
