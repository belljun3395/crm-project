package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.domain.WebhookResponse
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class WebhookQueryService(
    private val webhookRepository: WebhookRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun get(id: Long): WebhookResponse {
        val webhook = webhookRepository.findById(id) ?: throw NotFoundByIdException("Webhook", id)
        return out { webhook.toResponse(formatter) }
    }

    suspend fun list(): List<WebhookResponse> {
        return webhookRepository.findAll()
            .toList()
            .map { it.toResponse(formatter) }
    }

    private fun com.manage.crm.webhook.domain.Webhook.toResponse(dateTimeFormatter: DateTimeFormatter): WebhookResponse {
        return WebhookResponse(
            id = id!!,
            name = name,
            url = url,
            events = events.toValues(),
            active = active,
            createdAt = createdAt?.format(dateTimeFormatter)
        )
    }
}
