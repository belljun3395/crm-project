package com.manage.crm.email.event.template.handler

import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class PostEmailTemplateEventHandler(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    private val transactionalTemplates: TransactionTemplates
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * - Save Email Template History
     */
    suspend fun handle(event: PostEmailTemplateEvent) {
        scope.launch {
            transactionalTemplates.writer.executeAndAwait {
                val templateId = event.templateId
                val template =
                    emailTemplateRepository
                        .findById(templateId)
                        ?: throw IllegalArgumentException("EmailTemplate not found by id: $templateId")

                emailTemplateHistoryRepository.save(
                    EmailTemplateHistory(
                        templateId = template.id!!,
                        subject = template.subject,
                        body = template.body,
                        variables = template.variables,
                        version = template.version
                    )
                )
            }
        }
    }
}
