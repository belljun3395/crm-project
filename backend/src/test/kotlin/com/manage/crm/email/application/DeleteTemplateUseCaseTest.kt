package com.manage.crm.email.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseIn
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.domain.vo.ScheduleType
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

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

    fun scheduledEventStubs(templateId: Long, size: Int, objectMapper: ObjectMapper) =
        (1..size).map {
            ScheduledEvent.new(
                eventId = EventId("eventId$it"),
                eventClass = NotificationEmailSendTimeOutEvent::class.simpleName!!,
                eventPayload = objectMapper.writeValueAsString(
                    NotificationEmailSendTimeOutEvent(
                        eventId = EventId("eventId$it"),
                        templateId = templateId,
                        templateVersion = 1.0f,
                        userIds = listOf(1L),
                        expiredTime = LocalDateTime.now()
                    )
                ),
                completed = false,
                scheduledAt = ScheduleType.AWS.name
            )
        }

    given("DeleteTemplateUseCase") {
        val objectMapper = ObjectMapper().apply {
            registerModules(JavaTimeModule())
        }
        `when`("delete template with force flag") {
            val emailTemplateId = 1L
            val useCaseIn =
                DeleteTemplateUseCaseIn(emailTemplateId = emailTemplateId, forceFlag = true)

            val eventSize = 3
            val schedules = scheduledEventStubs(emailTemplateId, eventSize, objectMapper)
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
            val schedules = scheduledEventStubs(emailTemplateId, eventSize, objectMapper)
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
