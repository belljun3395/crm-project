package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.application.dto.GetWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.GetWebhookUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDto
import com.manage.crm.webhook.domain.repository.WebhookRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true")
class GetWebhookUseCase(
    private val webhookRepository: WebhookRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: GetWebhookUseCaseIn): GetWebhookUseCaseOut {
        val id = useCaseIn.id
        val webhook = webhookRepository.findById(id) ?: throw NotFoundByIdException("Webhook", id)

        return out {
            GetWebhookUseCaseOut(
                webhook = WebhookDto(
                    id = webhook.id!!,
                    name = webhook.name,
                    url = webhook.url,
                    events = webhook.events.toValues(),
                    active = webhook.active,
                    createdAt = webhook.createdAt?.format(formatter)
                )
            )
        }
    }
}
