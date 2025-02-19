package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.service.ScheduleTaskService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class PostEmailNotificationSchedulesUseCaseTest : BehaviorSpec({
    lateinit var scheduleTaskService: ScheduleTaskService
    lateinit var useCase: PostEmailNotificationSchedulesUseCase
    beforeContainer {
        scheduleTaskService = mockk()
        useCase = PostEmailNotificationSchedulesUseCase(scheduleTaskService)
    }

    given("PostEmailNotificationSchedulesUseCase") {
        `when`("post email notification schedules") {
            val useCaseIn = PostEmailNotificationSchedulesUseCaseIn(
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1, 2, 3),
                expiredTime = LocalDateTime.now().plusDays(1)
            )
            // case parameter's event id is not injected
            coEvery { scheduleTaskService.newSchedule(any()) } answers { "newSchedule" }

            val result = useCase.execute(useCaseIn)
            then("should return PostEmailNotificationSchedulesUseCaseOut") {
                result.newSchedule shouldBe "newSchedule"
            }

            then("create new schedule") {
                coVerify(exactly = 1) { scheduleTaskService.newSchedule(any()) }
            }
        }
    }
})
