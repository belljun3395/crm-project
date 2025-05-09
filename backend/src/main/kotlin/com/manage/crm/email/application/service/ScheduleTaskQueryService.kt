package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.ScheduleTaskView

/**
 * 스케줄 관련 조회 서비스를 제공하는 인터페이스
 */
interface ScheduleTaskQueryService {
    /**
     * 등록한 스케쥴 정보를 조회합니다.
     */
    suspend fun browseScheduledTasksView(): List<ScheduleTaskView>
}
