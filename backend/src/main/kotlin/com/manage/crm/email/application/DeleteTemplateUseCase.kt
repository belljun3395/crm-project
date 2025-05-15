package com.manage.crm.email.application

import com.manage.crm.email.application.dto.DeleteTemplateUseCaseIn
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseOut
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteTemplateUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val scheduledEventRepository: ScheduledEventRepository,
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskAllService
) {
    @Transactional
    suspend fun execute(useCaseIn: DeleteTemplateUseCaseIn): DeleteTemplateUseCaseOut {
        val emailTemplateId = useCaseIn.emailTemplateId
        val forceFlag = useCaseIn.forceFlag

        val schedules =
            scheduledEventRepository.findAllByEmailTemplateIdAndCompletedFalse(emailTemplateId)

        if (schedules.isNotEmpty() && !forceFlag) {
            return DeleteTemplateUseCaseOut(success = false)
        } else {
            schedules.forEach { schedule ->
                scheduleTaskService.cancel(schedule.eventId.value)
                schedule.cancel()
            }
            emailTemplateRepository.deleteById(emailTemplateId)
            return DeleteTemplateUseCaseOut(success = true)
        }
    }
}
