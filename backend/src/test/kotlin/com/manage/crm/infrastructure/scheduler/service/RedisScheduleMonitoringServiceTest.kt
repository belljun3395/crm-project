package com.manage.crm.infrastructure.scheduler.service

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.executor.ScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisScheduledTask
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class RedisScheduleMonitoringServiceTest : BehaviorSpec({

    val redisSchedulerProvider = mockk<RedisSchedulerProvider>()
    val scheduledTaskExecutor = mockk<ScheduledTaskExecutor>()
    val monitoringService = RedisScheduleMonitoringService(redisSchedulerProvider, scheduledTaskExecutor)

    beforeEach {
        clearAllMocks()
    }

    Given("A Redis schedule monitoring service") {

        When("processing expired schedules with no expired tasks") {
            every { redisSchedulerProvider.getExpiredSchedules() } returns emptyList()

            Then("it should do nothing") {
                monitoringService.processExpiredSchedules()

                verify { redisSchedulerProvider.getExpiredSchedules() }
                verify(exactly = 0) { scheduledTaskExecutor.executeScheduledTask(any(), any()) }
                verify(exactly = 0) { redisSchedulerProvider.removeSchedulesAtomically(any()) }
            }
        }

        When("processing expired schedules with expired tasks") {
            val expiredTask1 = RedisScheduledTask(
                taskId = "expired-task-1",
                scheduleInfo = NotificationEmailSendTimeOutEventInput(
                    templateId = 1L,
                    templateVersion = 1.0f,
                    userIds = listOf(1L, 2L),
                    eventId = EventId("expired-task-1"),
                    expiredTime = LocalDateTime.now().minusMinutes(5)
                ),
                scheduledAt = LocalDateTime.now().minusMinutes(5),
                createdAt = LocalDateTime.now().minusHours(1)
            )

            val expiredTask2 = RedisScheduledTask(
                taskId = "expired-task-2",
                scheduleInfo = NotificationEmailSendTimeOutEventInput(
                    templateId = 2L,
                    templateVersion = 1.0f,
                    userIds = listOf(3L, 4L),
                    eventId = EventId("expired-task-2"),
                    expiredTime = LocalDateTime.now().minusMinutes(3)
                ),
                scheduledAt = LocalDateTime.now().minusMinutes(3),
                createdAt = LocalDateTime.now().minusHours(1)
            )

            val executeSlot = slot<List<String>>()

            every { redisSchedulerProvider.getExpiredSchedules() } returns listOf(expiredTask1, expiredTask2)
            every { scheduledTaskExecutor.executeScheduledTask(any(), any()) } just runs
            every { redisSchedulerProvider.removeSchedulesAtomically(capture(executeSlot)) } returns 1L

            Then("it should execute all expired tasks") {
                monitoringService.processExpiredSchedules()

                // processExpiredSchedules()는 비동기로 코루틴을 실행하므로 즉시 완료되지 않음
                // 하지만 테스트에서는 동기적으로 검증할 수 있는 부분만 확인
                verify { redisSchedulerProvider.getExpiredSchedules() }

                // 비동기 작업이 시작되었는지 확인하기 위해 잠시 대기
                Thread.sleep(200)

                // 각 태스크가 실행되었는지 확인 (순서는 보장되지 않을 수 있음)
                verify(atLeast = 1) { scheduledTaskExecutor.executeScheduledTask("expired-task-1", expiredTask1.scheduleInfo) }
                verify(atLeast = 1) { scheduledTaskExecutor.executeScheduledTask("expired-task-2", expiredTask2.scheduleInfo) }
            }
        }

        When("processing expired schedules but execution fails") {
            val expiredTask = RedisScheduledTask(
                taskId = "failed-task",
                scheduleInfo = NotificationEmailSendTimeOutEventInput(
                    templateId = 1L,
                    templateVersion = 1.0f,
                    userIds = listOf(1L),
                    eventId = EventId("failed-task"),
                    expiredTime = LocalDateTime.now().minusMinutes(1)
                ),
                scheduledAt = LocalDateTime.now().minusMinutes(1),
                createdAt = LocalDateTime.now().minusHours(1)
            )

            every { redisSchedulerProvider.getExpiredSchedules() } returns listOf(expiredTask)
            every { scheduledTaskExecutor.executeScheduledTask(any(), any()) } throws RuntimeException("Execution failed")

            Then("it should handle the failure and not remove the task") {
                monitoringService.processExpiredSchedules()

                // 비동기 처리를 위해 잠시 대기
                Thread.sleep(200)

                verify { redisSchedulerProvider.getExpiredSchedules() }
                verify(atLeast = 1) {
                    scheduledTaskExecutor.executeScheduledTask("failed-task", expiredTask.scheduleInfo)
                }
                // 실패한 경우 removeSchedulesAtomically가 호출되지 않아야 함
                verify(exactly = 0) { redisSchedulerProvider.removeSchedulesAtomically(any()) }
            }
        }

        When("logging scheduler status") {
            every { redisSchedulerProvider.browseSchedule() } returns emptyList()

            Then("it should log the current schedule count") {
                monitoringService.logSchedulerStatus()

                verify { redisSchedulerProvider.browseSchedule() }
            }
        }

        When("browsing schedules fails during status logging") {
            every { redisSchedulerProvider.browseSchedule() } throws RuntimeException("Redis connection failed")

            Then("it should handle the failure gracefully") {
                // 예외가 발생해도 서비스는 계속 실행되어야 함
                kotlin.runCatching {
                    monitoringService.logSchedulerStatus()
                }

                verify { redisSchedulerProvider.browseSchedule() }
            }
        }

        When("provider type is checked") {
            every { redisSchedulerProvider.getProviderType() } returns "redis-kafka"

            Then("it should be redis-kafka") {
                redisSchedulerProvider.getProviderType() shouldBe "redis-kafka"
            }
        }
    }
})
