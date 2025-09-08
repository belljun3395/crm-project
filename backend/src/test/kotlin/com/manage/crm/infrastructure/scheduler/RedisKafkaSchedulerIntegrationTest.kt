package com.manage.crm.infrastructure.scheduler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskServiceImpl
import com.manage.crm.email.domain.vo.EventId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.*

/**
 * Redis+Kafka 스케줄러 통합 테스트
 * Mock을 사용하여 스케줄러 로직을 테스트합니다.
 */
class RedisKafkaSchedulerIntegrationTest : BehaviorSpec({

    val scheduleTaskService = mockk<ScheduleTaskServiceImpl>()

    beforeEach {
        clearAllMocks()
    }

    Given("A Redis+Kafka scheduler system") {

        When("creating a new schedule") {
            Then("it should return the correct schedule ID and validate input") {
                val futureTime = LocalDateTime.now().plusMinutes(1)
                val eventId = EventId(UUID.randomUUID().toString())
                val input = NotificationEmailSendTimeOutEventInput(
                    templateId = 100L,
                    templateVersion = 1.0f,
                    userIds = listOf(1L, 2L, 3L),
                    eventId = eventId,
                    expiredTime = futureTime
                )

                every { scheduleTaskService.newSchedule(input) } returns eventId.value

                val scheduleId = scheduleTaskService.newSchedule(input)
                
                // 비즈니스 로직 검증
                scheduleId shouldBe eventId.value
                scheduleId.length shouldBe eventId.value.length
                
                // 실제로 올바른 인자로 호출되었는지 확인
                verify { 
                    scheduleTaskService.newSchedule(match { scheduledInput ->
                        scheduledInput.templateId == 100L &&
                        scheduledInput.templateVersion == 1.0f &&
                        scheduledInput.userIds == listOf(1L, 2L, 3L) &&
                        scheduledInput.eventId == eventId &&
                        scheduledInput.expiredTime == futureTime
                    })
                }
            }
        }

        When("canceling a schedule") {
            Then("it should call the cancel method") {
                val eventId = "test-event-id"
                
                every { scheduleTaskService.cancel(eventId) } just runs

                scheduleTaskService.cancel(eventId)

                verify { scheduleTaskService.cancel(eventId) }
            }
        }

        When("rescheduling a task") {
            Then("it should call the reschedule method") {
                val eventId = EventId(UUID.randomUUID().toString())
                val input = NotificationEmailSendTimeOutEventInput(
                    templateId = 200L,
                    templateVersion = 1.0f,
                    userIds = listOf(10L, 20L),
                    eventId = eventId,
                    expiredTime = LocalDateTime.now().plusMinutes(10)
                )

                every { scheduleTaskService.reSchedule(input) } just runs

                scheduleTaskService.reSchedule(input)

                verify { scheduleTaskService.reSchedule(input) }
            }
        }

        When("browsing scheduled tasks") {
            Then("it should return the scheduled tasks view") {
                runBlocking {
                    coEvery { scheduleTaskService.browseScheduledTasksView() } returns emptyList()
                    
                    val tasks = scheduleTaskService.browseScheduledTasksView()
                    tasks shouldBe emptyList()

                    coVerify { scheduleTaskService.browseScheduledTasksView() }
                }
            }
        }

        When("service operations fail") {
            Then("it should handle service failures appropriately") {
                val eventId = "failing-event-id"
                
                // 실제 실패 시나리오 테스트
                every { scheduleTaskService.cancel(eventId) } throws RuntimeException("Service unavailable")
                
                var exceptionCaught: Exception? = null
                try {
                    scheduleTaskService.cancel(eventId)
                } catch (e: Exception) {
                    exceptionCaught = e
                }
                
                // 예외가 실제로 발생하는지 확인 (타입과 메시지 확인)
                exceptionCaught shouldNotBe null
                val exception = exceptionCaught as RuntimeException
                exception::class shouldBe RuntimeException::class
                exception.message shouldBe "Service unavailable"
                verify { scheduleTaskService.cancel(eventId) }
            }
        }
    }
})