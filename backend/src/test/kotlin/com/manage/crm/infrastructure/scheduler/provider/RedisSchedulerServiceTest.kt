package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate

class RedisSchedulerServiceTest : BehaviorSpec({

    val redisTemplate = mockk<RedisTemplate<String, Any>>()
    val objectMapper = mockk<ObjectMapper>()
    val redisSchedulerService = RedisSchedulerService(redisTemplate, objectMapper)

    Given("A Redis scheduler service") {

        When("getting and removing expired schedules with Lua script") {
            val currentTime = System.currentTimeMillis() / 1000.0
            val expiredTaskIds = listOf("task1", "task2", "task3")

            every { 
                redisTemplate.execute(
                    any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                    any<List<String>>(),
                    *anyVararg<String>()
                )
            } returns expiredTaskIds

            Then("it should return expired task IDs and remove them atomically") {
                val result = redisSchedulerService.getAndRemoveExpiredSchedules(currentTime)

                result shouldBe setOf("task1", "task2", "task3")
                verify { 
                    redisTemplate.execute(
                        any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                        any<List<String>>(),
                        *anyVararg<String>()
                    )
                }
            }
        }

        When("getting and removing expired schedules with no results") {
            val currentTime = System.currentTimeMillis() / 1000.0

            every { 
                redisTemplate.execute(
                    any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                    any<List<String>>(),
                    *anyVararg<String>()
                )
            } returns emptyList<String>()

            Then("it should return empty set") {
                val result = redisSchedulerService.getAndRemoveExpiredSchedules(currentTime)

                result.shouldBeEmpty()
                verify { 
                    redisTemplate.execute(
                        any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                        any<List<String>>(),
                        *anyVararg<String>()
                    )
                }
            }
        }

        When("Lua script execution returns null") {
            val currentTime = System.currentTimeMillis() / 1000.0

            every { 
                redisTemplate.execute(
                    any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                    any<List<String>>(),
                    *anyVararg<String>()
                )
            } returns emptyList<String>()

            Then("it should return empty set") {
                val result = redisSchedulerService.getAndRemoveExpiredSchedules(currentTime)

                result.shouldBeEmpty()
            }
        }

        When("getting task data") {
            val taskId = "test-task-123"
            val taskData = """{"taskId":"test-task-123","scheduleInfo":{}}"""

            every { redisTemplate.opsForValue().get("scheduled:task:$taskId") } returns taskData

            Then("it should return task data") {
                val result = redisSchedulerService.getTaskData(taskId)

                result shouldBe taskData
                verify { redisTemplate.opsForValue().get("scheduled:task:$taskId") }
            }
        }

        When("setting task data") {
            val taskId = "test-task-456"
            val taskData = """{"taskId":"test-task-456","scheduleInfo":{}}"""

            every { redisTemplate.opsForValue().set(any<String>(), any<String>()) } just Runs

            Then("it should store task data with correct key") {
                redisSchedulerService.setTaskData(taskId, taskData)

                verify { redisTemplate.opsForValue().set("scheduled:task:$taskId", taskData) }
            }
        }

        When("removing task data") {
            val taskId = "test-task-789"

            every { redisTemplate.delete(any<String>()) } returns true

            Then("it should delete task data with correct key") {
                redisSchedulerService.removeTaskData(taskId)

                verify { redisTemplate.delete("scheduled:task:$taskId") }
            }
        }
    }
})
