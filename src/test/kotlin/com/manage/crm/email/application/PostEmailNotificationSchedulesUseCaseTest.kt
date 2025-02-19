package com.manage.crm.email.application

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.email.domain.vo.EventId
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

            val eventId = EventId()
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1, 2, 3),
                eventId = eventId,
                expiredTime = LocalDateTime.now().plusDays(1)
            )
            coEvery { scheduleTaskService.newSchedule(any(NotificationEmailSendTimeOutEventInput::class)) } answers { input.eventId.value }

            val result = useCase.execute(useCaseIn)
            then("should return PostEmailNotificationSchedulesUseCaseOut") {
                result.newSchedule shouldBe eventId.value
            }

            then("create new schedule") {
                coVerify(exactly = 1) { scheduleTaskService.newSchedule(any(NotificationEmailSendTimeOutEventInput::class)) }
            }
        }
    }
})
