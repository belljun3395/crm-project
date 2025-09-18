package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * AWS EventBridge 기반 스케줄러 제공자
 * 기존 AwsSchedulerService를 SchedulerProvider 인터페이스로 감싸는 어댑터
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws", matchIfMissing = true)
class AwsSchedulerProvider(
    private val awsSchedulerService: AwsSchedulerService
) : SchedulerProvider {

    override fun createSchedule(name: String, schedule: LocalDateTime, input: ScheduleInfo): String {
        val response = awsSchedulerService.createSchedule(name, schedule, input)
        return response.scheduleArn()
    }

    override fun browseSchedule(): List<ScheduleName> {
        return awsSchedulerService.browseSchedule()
    }

    override fun deleteSchedule(scheduleName: ScheduleName) {
        awsSchedulerService.deleteSchedule(scheduleName)
    }

    override fun getProviderType(): String = "aws"
}
