package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.time.ZoneOffset

class RedisSchedulerProviderTest : BehaviorSpec({

    val redisSchedulerService = mockk<RedisSchedulerService>()

    // ObjectMapper에 필요한 모듈 추가
    val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }

    val redisSchedulerProvider = RedisSchedulerProvider(redisSchedulerService, objectMapper)

    Given("A Redis scheduler provider") {

        When("creating a new schedule") {
            val scheduleTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L, 3L),
                eventId = EventId("test-event-123"),
                expiredTime = scheduleTime
            )

            val jsonSlot = slot<String>()
            every { redisSchedulerService.setTaskData(any(), capture(jsonSlot)) } returns Unit
            every { redisSchedulerService.addTaskToScheduledTasks(any(), any()) } returns Unit

            Then("it should create the schedule successfully") {
                val result = redisSchedulerProvider.createSchedule("test-task", scheduleTime, input)

                result shouldBe "test-task"

                val expectedScore = scheduleTime.toEpochSecond(ZoneOffset.UTC).toDouble()
                verify { redisSchedulerService.addTaskToScheduledTasks("test-task", expectedScore) }
                verify { redisSchedulerService.setTaskData("test-task", any()) }

                // JSON 직렬화가 올바른지 확인
                val savedJson = jsonSlot.captured
                val savedTask = objectMapper.readValue(savedJson, RedisScheduledTask::class.java)
                savedTask.taskId shouldBe "test-task"
                savedTask.scheduleInfo shouldBe input
                savedTask.scheduledAt shouldBe scheduleTime
                // createdAt이 현재 시간 근처인지 확인
                savedTask.createdAt.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
            }
        }

        When("browsing schedules") {
            val mockScheduleIds = setOf("task-1", "task-2", "task-3")
            every { redisSchedulerService.browseAllTasksInScheduledTasks() } returns mockScheduleIds

            Then("it should return all scheduled tasks") {
                val result = redisSchedulerProvider.browseSchedule()

                result shouldHaveSize 3
                result.map { it.value } shouldBe listOf("task-1", "task-2", "task-3")
            }
        }

        When("deleting a schedule") {
            every { redisSchedulerService.removeTaskInScheduledTasks("test-task") } returns Unit
            every { redisSchedulerService.removeTaskData("test-task") } returns Unit

            Then("it should remove the schedule from Redis") {
                redisSchedulerProvider.deleteSchedule(ScheduleName("test-task"))

                verify { redisSchedulerService.removeTaskInScheduledTasks("test-task") }
                verify { redisSchedulerService.removeTaskData("test-task") }
            }
        }

        When("getting expired schedules") {
            val expiredTaskIds = setOf(1L, 2L)

            // 실제 구조와 맞는 JSON 데이터 생성
            val scheduleInfo = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("expired-task-1"),
                expiredTime = LocalDateTime.of(2024, 12, 31, 12, 0, 0)
            )
            val redisTask = RedisScheduledTask(
                taskId = "1",
                scheduleInfo = scheduleInfo,
                scheduledAt = LocalDateTime.of(2024, 12, 31, 12, 0, 0),
                createdAt = LocalDateTime.of(2024, 12, 31, 11, 0, 0)
            )
            val taskDataJson = objectMapper.writeValueAsString(redisTask)

            // browseDueTaskIdsInScheduledTasks를 올바르게 mock
            every { redisSchedulerService.browseDueTaskIdsInScheduledTasks(any<Double>()) } returns expiredTaskIds
            every { redisSchedulerService.getTaskData("1") } returns taskDataJson
            every { redisSchedulerService.getTaskData("2") } returns null

            Then("it should return expired tasks") {
                val result = redisSchedulerProvider.getExpiredSchedules()

                result shouldHaveSize 1
                val firstResult = result.first()
                firstResult.taskId shouldBe "1"
                firstResult.scheduledAt shouldBe LocalDateTime.of(2024, 12, 31, 12, 0, 0)
                firstResult.createdAt shouldBe LocalDateTime.of(2024, 12, 31, 11, 0, 0)

                // scheduleInfo가 올바른 타입인지 확인
                firstResult.scheduleInfo.shouldBeInstanceOf<NotificationEmailSendTimeOutEventInput>()
                val deserializedInput = firstResult.scheduleInfo as NotificationEmailSendTimeOutEventInput
                deserializedInput.templateId shouldBe 1L
                deserializedInput.templateVersion shouldBe 1.0f
                deserializedInput.userIds shouldBe listOf(1L)
                deserializedInput.eventId.value shouldBe "expired-task-1"
                deserializedInput.expiredTime shouldBe LocalDateTime.of(2024, 12, 31, 12, 0, 0)
            }
        }

        When("no schedules exist") {
            every { redisSchedulerService.browseAllTasksInScheduledTasks() } returns emptySet()

            Then("browsing should return empty list") {
                val result = redisSchedulerProvider.browseSchedule()
                result.shouldBeEmpty()
            }
        }

        When("Redis operations fail") {
            every { redisSchedulerService.setTaskData(any(), any()) } returns Unit
            every { redisSchedulerService.addTaskToScheduledTasks(any(), any()) } throws RuntimeException("Redis connection failed")

            Then("it should throw a RuntimeException") {
                val scheduleTime = LocalDateTime.now().plusHours(1)
                val input = NotificationEmailSendTimeOutEventInput(
                    templateId = 1L,
                    templateVersion = 1.0f,
                    userIds = listOf(1L),
                    eventId = EventId("failed-event"),
                    expiredTime = scheduleTime
                )

                val exception = kotlin.runCatching {
                    redisSchedulerProvider.createSchedule("failed-task", scheduleTime, input)
                }.exceptionOrNull()

                exception!!.shouldBeInstanceOf<RuntimeException>()
                exception.message shouldBe "Error creating Redis schedule: failed-task"
                exception.cause?.message shouldBe "Redis connection failed"
            }
        }

        When("atomically removing schedules") {
            val taskIds = listOf("task-1", "task-2")

            every { redisSchedulerService.removeTaskIdFromScheduledTasks("task-1") } returns 1L
            every { redisSchedulerService.removeTaskIdFromScheduledTasks("task-2") } returns 1L
            every { redisSchedulerService.removeTaskData("task-1") } returns Unit
            every { redisSchedulerService.removeTaskData("task-2") } returns Unit

            Then("it should remove schedules one by one") {
                val result = redisSchedulerProvider.removeSchedulesAtomically(taskIds)

                result shouldBe 2L
                verify { redisSchedulerService.removeTaskIdFromScheduledTasks("task-1") }
                verify { redisSchedulerService.removeTaskIdFromScheduledTasks("task-2") }
                verify { redisSchedulerService.removeTaskData("task-1") }
                verify { redisSchedulerService.removeTaskData("task-2") }
            }
        }

        When("atomically removing empty task list") {
            Then("it should return zero without Redis call") {
                // 빈 리스트에 대해서는 early return되므로 Redis 호출이 없어야 함
                val result = redisSchedulerProvider.removeSchedulesAtomically(emptyList())

                result shouldBe 0L
                // 이 경우에는 Redis operations가 호출되지 않으므로 verify 없이 결과만 확인
            }
        }
    }
})
