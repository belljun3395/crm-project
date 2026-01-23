package com.manage.crm.webhook.application

import com.manage.crm.support.out
import com.manage.crm.webhook.application.dto.BrowseWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.BrowseWebhookUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDto
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class BrowseWebhookUseCase(
    private val webhookRepository: WebhookRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun execute(useCaseIn: BrowseWebhookUseCaseIn): BrowseWebhookUseCaseOut {
        val webhooks = webhookRepository.findAll()
            .toList()
            .map {
                WebhookDto(
                    id = it.id!!,
                    name = it.name,
                    url = it.url,
                    events = it.events.toValues(),
                    active = it.active,
                    createdAt = it.createdAt?.format(formatter)
                )
            }

        return out {
            BrowseWebhookUseCaseOut(webhooks)
        }
    }
}
