package com.manage.crm.email.application.service

import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import com.manage.crm.email.support.EmailEventPublisher
import org.springframework.stereotype.Component

@Component
class EmailTemplateRepositoryEventProcessor(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailEventPublisher: EmailEventPublisher
) {
    suspend fun save(emailTemplate: EmailTemplate): EmailTemplate {
        val domainEvents = emailTemplate.domainEvents
        val template = if (emailTemplate.isNewTemplate()) {
            emailTemplateRepository.save(emailTemplate)
                .apply { domainEvents.add(PostEmailTemplateEvent(templateId = this.id!!)) }
        } else {
            emailTemplateRepository.save(emailTemplate)
        }

        emailEventPublisher.publishEvent(domainEvents)

        return template
    }
}
