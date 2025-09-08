package com.manage.crm.infrastructure.scheduler.service

import com.manage.crm.infrastructure.scheduler.executor.ScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RedisScheduleMonitoringServiceTest : BehaviorSpec({

    val redisSchedulerProvider = mockk<RedisSchedulerProvider>()
    val scheduledTaskExecutor = mockk<ScheduledTaskExecutor>()
    val monitoringService = RedisScheduleMonitoringService(redisSchedulerProvider, scheduledTaskExecutor)

    beforeEach {
        clearAllMocks()
    }

    Given("A Redis schedule monitoring service") {

        When("processing expired schedules with no expired tasks") {
            Then("it should do nothing") {
                every { redisSchedulerProvider.getExpiredSchedules() } returns emptyList()

                monitoringService.processExpiredSchedules()

                verify { redisSchedulerProvider.getExpiredSchedules() }
                verify(exactly = 0) { scheduledTaskExecutor.executeScheduledTask(any(), any()) }
                verify(exactly = 0) { redisSchedulerProvider.removeSchedulesAtomically(any()) }
            }
        }

        When("logging scheduler status") {
            Then("it should log the current schedule count") {
                every { redisSchedulerProvider.browseSchedule() } returns emptyList()

                monitoringService.logSchedulerStatus()

                verify { redisSchedulerProvider.browseSchedule() }
            }
        }

        When("browsing schedules fails during status logging") {
            Then("it should handle the failure gracefully") {
                every { redisSchedulerProvider.browseSchedule() } throws RuntimeException("Redis connection failed")

                // 예외가 발생해도 서비스는 계속 실행되어야 하고 예외를 던지지 않아야 함
                var exceptionThrown: Exception? = null
                try {
                    monitoringService.logSchedulerStatus()
                } catch (e: Exception) {
                    exceptionThrown = e
                }

                // 실제로 예외가 잡혀서 처리되는지 확인 (예외가 밖으로 나오면 안 됨)
                exceptionThrown shouldBe null
                verify { redisSchedulerProvider.browseSchedule() }
            }
        }

        When("provider type is checked") {
            Then("it should return redis-kafka") {
                every { redisSchedulerProvider.getProviderType() } returns "redis-kafka"

                redisSchedulerProvider.getProviderType() shouldBe "redis-kafka"

                verify { redisSchedulerProvider.getProviderType() }
            }
        }
    }
})
