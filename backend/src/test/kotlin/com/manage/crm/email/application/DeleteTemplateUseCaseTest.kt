package com.manage.crm.email.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseIn
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.ScheduledEventFixtures
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class DeleteTemplateUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var scheduledEventRepository: ScheduledEventRepository
    lateinit var scheduleTaskService: ScheduleTaskAllService
    lateinit var deleteTemplateUseCase: DeleteTemplateUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        scheduledEventRepository = mockk()
        scheduleTaskService = mockk()
        deleteTemplateUseCase =
            DeleteTemplateUseCase(
                emailTemplateRepository,
                scheduledEventRepository,
                scheduleTaskService
            )
    }

    fun scheduledEventStubs(size: Int) =
        (1..size).map { ScheduledEventFixtures.giveMeOne().build() }

    given("DeleteTemplateUseCase") {
        val objectMapper = ObjectMapper().apply {
            registerModules(JavaTimeModule())
        }
        `when`("delete template with force flag") {
            val emailTemplateId = 1L
            val useCaseIn =
                DeleteTemplateUseCaseIn(emailTemplateId = emailTemplateId, forceFlag = true)

            val eventSize = 3
            val schedules = scheduledEventStubs(eventSize)
            coEvery {
                scheduledEventRepository.findAllByEmailTemplateIdAndCompletedFalse(
                    emailTemplateId
                )
            } returns schedules

            coEvery { scheduleTaskService.cancel(any()) } returns Unit

            coEvery { emailTemplateRepository.deleteById(any()) } returns Unit

            val result = deleteTemplateUseCase.execute(useCaseIn)
            then("should return DeleteTemplateUseCaseOut") {
                result shouldBe DeleteTemplateUseCaseOut(success = true)
            }

            then("find all schedules related to email template") {
                coVerify(exactly = 1) {
                    scheduledEventRepository.findAllByEmailTemplateIdAndCompletedFalse(
                        emailTemplateId
                    )
                }
            }

            then("cancel all scheduled tasks") {
                coVerify(exactly = eventSize) { scheduleTaskService.cancel(any()) }
            }

            then("delete email template") {
                coVerify(exactly = 1) { emailTemplateRepository.deleteById(emailTemplateId) }
            }
        }

        `when`("force flag is false and there are schedules") {
            val emailTemplateId = 1L
            val useCaseIn =
                DeleteTemplateUseCaseIn(emailTemplateId = emailTemplateId, forceFlag = false)

            val eventSize = 3
            val schedules = scheduledEventStubs(eventSize)
            coEvery {
                scheduledEventRepository.findAllByEmailTemplateIdAndCompletedFalse(
                    emailTemplateId
                )
            } returns schedules

            val result = deleteTemplateUseCase.execute(useCaseIn)
            then("should return DeleteTemplateUseCaseOut") {
                result shouldBe DeleteTemplateUseCaseOut(success = false)
            }
        }

        `when`("force flag is false and there are no schedules") {
            val emailTemplateId = 1L
            val useCaseIn =
                DeleteTemplateUseCaseIn(emailTemplateId = emailTemplateId, forceFlag = false)

            coEvery {
                scheduledEventRepository.findAllByEmailTemplateIdAndCompletedFalse(
                    emailTemplateId
                )
            } returns emptyList()

            coEvery { emailTemplateRepository.deleteById(any()) } returns Unit

            val result = deleteTemplateUseCase.execute(useCaseIn)
            then("should return DeleteTemplateUseCaseOut") {
                result shouldBe DeleteTemplateUseCaseOut(success = true)
            }

            then("not cancel any scheduled tasks") {
                coVerify(exactly = 0) { scheduleTaskService.cancel(any()) }
            }

            then("delete email template") {
                coVerify(exactly = 1) { emailTemplateRepository.deleteById(emailTemplateId) }
            }
        }
    }
})
