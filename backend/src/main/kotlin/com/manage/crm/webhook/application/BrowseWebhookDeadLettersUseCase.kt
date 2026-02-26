package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.application.dto.BrowseWebhookDeadLettersUseCaseIn
import com.manage.crm.webhook.application.dto.BrowseWebhookDeadLettersUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDeadLetterDto
import com.manage.crm.webhook.domain.repository.WebhookDeadLetterRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class BrowseWebhookDeadLettersUseCase(
    private val webhookRepository: WebhookRepository,
    private val webhookDeadLetterRepository: WebhookDeadLetterRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseWebhookDeadLettersUseCaseIn): BrowseWebhookDeadLettersUseCaseOut {
        val webhookId = useCaseIn.webhookId
        webhookRepository.findById(webhookId) ?: throw NotFoundByIdException("Webhook", webhookId)

        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val deadLetters = webhookDeadLetterRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId)
            .take(normalizedLimit)
            .toList()
            .map { deadLetter ->
                WebhookDeadLetterDto(
                    id = deadLetter.id!!,
                    webhookId = deadLetter.webhookId,
                    eventId = deadLetter.eventId,
                    eventType = deadLetter.eventType,
                    payloadJson = deadLetter.payloadJson,
                    deliveryStatus = deadLetter.deliveryStatus,
                    attemptCount = deadLetter.attemptCount,
                    responseStatus = deadLetter.responseStatus,
                    errorMessage = deadLetter.errorMessage,
                    createdAt = deadLetter.createdAt?.format(formatter)
                )
            }

        return out {
            BrowseWebhookDeadLettersUseCaseOut(deadLetters)
        }
    }
}
