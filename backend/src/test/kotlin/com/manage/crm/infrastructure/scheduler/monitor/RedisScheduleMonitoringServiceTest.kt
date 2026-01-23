package com.manage.crm.infrastructure.scheduler.monitor

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.executor.KafkaScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RedisScheduleMonitoringServiceTest {

    private val redisSchedulerProvider: RedisSchedulerProvider = mockk(relaxed = true)
    private val kafkaExecutor: KafkaScheduledTaskExecutor = mockk()

    private val monitoringService = RedisScheduleMonitoringService(redisSchedulerProvider, kafkaExecutor)

    private fun sampleSchedule(name: String = "schedule-1"): RedisSchedulerProvider.ScheduleData {
        return RedisSchedulerProvider.ScheduleData(
            name = name,
            scheduleTime = LocalDateTime.now(),
            payload = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L),
                eventId = EventId(name),
                expiredTime = LocalDateTime.now()
            )
        )
    }

    @Test
    fun `processes due schedules and deletes after successful publish`() = runTest {
        val schedule = sampleSchedule()
        coEvery { redisSchedulerProvider.fetchDueSchedules() } returns listOf(schedule)
        coEvery { kafkaExecutor.execute(schedule) } returns true

        monitoringService.triggerProcessing()

        coVerify(exactly = 1) { kafkaExecutor.execute(schedule) }
        coVerify(exactly = 1) { redisSchedulerProvider.deleteSchedule(match { it.value == schedule.name }) }
    }

    @Test
    fun `does not delete when kafka publish fails`() = runTest {
        val schedule = sampleSchedule("fail-schedule")
        coEvery { redisSchedulerProvider.fetchDueSchedules() } returns listOf(schedule)
        coEvery { kafkaExecutor.execute(schedule) } returns false

        monitoringService.triggerProcessing()

        coVerify(exactly = 1) { kafkaExecutor.execute(schedule) }
        coVerify(exactly = 0) { redisSchedulerProvider.deleteSchedule(any()) }
    }

    @Test
    fun `no action when no due schedules`() = runTest {
        coEvery { redisSchedulerProvider.fetchDueSchedules() } returns emptyList()

        monitoringService.triggerProcessing()

        coVerify(exactly = 0) { kafkaExecutor.execute(any()) }
        coVerify(exactly = 0) { redisSchedulerProvider.deleteSchedule(any()) }
    }
}
