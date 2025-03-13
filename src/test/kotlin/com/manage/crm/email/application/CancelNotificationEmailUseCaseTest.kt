package com.manage.crm.email.application

import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskServicePostEventProcessor
import com.manage.crm.email.domain.vo.EventId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class CancelNotificationEmailUseCaseTest : BehaviorSpec({
    lateinit var scheduleTaskService: ScheduleTaskServicePostEventProcessor
    lateinit var cancelNotificationEmailUseCase: CancelNotificationEmailUseCase

    beforeContainer {
        scheduleTaskService = mockk()
        cancelNotificationEmailUseCase = CancelNotificationEmailUseCase(scheduleTaskService)
    }

    given("CancelNotificationEmailUseCase") {
        `when`("cancel notification email") {
            val useCaseIn = CancelNotificationEmailUseCaseIn(eventId = EventId("eventId"))

            coEvery { scheduleTaskService.cancel(any()) } returns Unit

            val result = cancelNotificationEmailUseCase.execute(useCaseIn)
            then("should return CancelNotificationEmailUseCaseOut") {
                result shouldBe CancelNotificationEmailUseCaseOut(true)
            }

            then("cancel notification email scheduled task") {
                coVerify(exactly = 1) { scheduleTaskService.cancel(useCaseIn.eventId.value) }
            }
        }
    }
})
