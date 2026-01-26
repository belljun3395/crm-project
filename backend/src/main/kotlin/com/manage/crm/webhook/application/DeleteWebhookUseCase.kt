package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.DeleteWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.DeleteWebhookUseCaseOut
import com.manage.crm.webhook.domain.repository.WebhookRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true")
class DeleteWebhookUseCase(
    private val webhookRepository: WebhookRepository
) {
    @Transactional
    suspend fun execute(useCaseIn: DeleteWebhookUseCaseIn): DeleteWebhookUseCaseOut {
        val id = useCaseIn.id
        val existing = webhookRepository.findById(id) ?: throw NotFoundByIdException("Webhook", id)
        webhookRepository.delete(existing)
        return DeleteWebhookUseCaseOut(success = true)
    }
}
