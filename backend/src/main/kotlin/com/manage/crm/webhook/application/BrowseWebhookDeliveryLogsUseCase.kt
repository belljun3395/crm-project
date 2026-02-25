package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.application.dto.BrowseWebhookDeliveryLogsUseCaseIn
import com.manage.crm.webhook.application.dto.BrowseWebhookDeliveryLogsUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDeliveryLogDto
import com.manage.crm.webhook.domain.repository.WebhookDeliveryLogRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
/**
 * Retrieves recent delivery attempts for a webhook in reverse chronological order.
 */
class BrowseWebhookDeliveryLogsUseCase(
    private val webhookRepository: WebhookRepository,
    private val webhookDeliveryLogRepository: WebhookDeliveryLogRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseWebhookDeliveryLogsUseCaseIn): BrowseWebhookDeliveryLogsUseCaseOut {
        val webhookId = useCaseIn.webhookId
        webhookRepository.findById(webhookId) ?: throw NotFoundByIdException("Webhook", webhookId)

        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val deliveries = webhookDeliveryLogRepository.findByWebhookIdOrderByDeliveredAtDesc(webhookId)
            .take(normalizedLimit)
            .toList()
            .map { delivery ->
                WebhookDeliveryLogDto(
                    id = delivery.id!!,
                    webhookId = delivery.webhookId,
                    eventId = delivery.eventId,
                    eventType = delivery.eventType,
                    deliveryStatus = delivery.deliveryStatus,
                    attemptCount = delivery.attemptCount,
                    responseStatus = delivery.responseStatus,
                    errorMessage = delivery.errorMessage,
                    deliveredAt = delivery.deliveredAt?.format(formatter)
                )
            }

        return out {
            BrowseWebhookDeliveryLogsUseCaseOut(deliveries)
        }
    }
}
