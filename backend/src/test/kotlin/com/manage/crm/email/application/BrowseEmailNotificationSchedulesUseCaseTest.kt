package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.EmailNotificationScheduleDto
import com.manage.crm.email.application.dto.ScheduleTaskView
import com.manage.crm.email.application.service.ScheduleTaskQueryService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlin.random.Random

class BrowseEmailNotificationSchedulesUseCaseTest : BehaviorSpec({
    lateinit var scheduleTaskService: ScheduleTaskQueryService
    lateinit var useCase: BrowseEmailNotificationSchedulesUseCase

    beforeContainer {
        scheduleTaskService = mockk()
        useCase = BrowseEmailNotificationSchedulesUseCase(scheduleTaskService)
    }

    fun scheduleTaskViewStubs(size: Int) = (1..size).map { it ->
        ScheduleTaskView(
            campaignId = it.toLong(),
            taskName = "task$it",
            templateId = it.toLong(),
            userIds = (1..it).map { it.toLong() },
            expiredTime = LocalDateTime.now().plusDays(it.toLong())
        )
    }

    given("BrowseEmailNotificationSchedulesUseCase") {
        `when`("browse all schedules") {
            val scheduleTaskViewSize = Random(1).nextInt(1, 10)
            val scheduleTaskViewStubs = scheduleTaskViewStubs(scheduleTaskViewSize)
            coEvery { scheduleTaskService.browseScheduledTasksView() } answers { scheduleTaskViewStubs }

            val result = useCase.execute()
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe BrowseEmailNotificationSchedulesUseCaseOut(
                    schedules = scheduleTaskViewStubs.map {
                        EmailNotificationScheduleDto(
                            taskName = it.taskName,
                            templateId = it.templateId,
                            userIds = it.userIds,
                            expiredTime = it.expiredTime
                        )
                    }
                )
            }

            then("browse all schedules") {
                coVerify(exactly = 1) { scheduleTaskService.browseScheduledTasksView() }
            }
        }
    }
})
