package com.manage.crm.webhook.application

import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.domain.CreateWebhookRequest
import com.manage.crm.webhook.domain.UpdateWebhookRequest
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.WebhookResponse
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class ManageWebhookUseCase(
    private val webhookRepository: WebhookRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun create(request: CreateWebhookRequest): WebhookResponse {
        val events = WebhookEvents.fromValues(request.events)
        webhookRepository.findByName(request.name)?.let {
            throw AlreadyExistsException("Webhook", "name", request.name)
        }

        val saved = try {
            webhookRepository.save(
                Webhook.new(
                    name = request.name,
                    url = request.url,
                    events = events,
                    active = request.active ?: true
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw AlreadyExistsException("Webhook", "name", request.name)
        }

        return out { saved.toResponse(formatter) }
    }

    suspend fun update(id: Long, request: UpdateWebhookRequest): WebhookResponse {
        val existing = webhookRepository.findById(id)
            ?: throw NotFoundByIdException("Webhook", id)

        request.name?.let { existing.name = it }
        request.url?.let { existing.url = it }
        request.events?.let { existing.events = WebhookEvents.fromValues(it) }
        request.active?.let { existing.active = it }

        val saved = webhookRepository.save(existing)
        return out { saved.toResponse(formatter) }
    }

    suspend fun delete(id: Long) {
        val existing = webhookRepository.findById(id) ?: throw NotFoundByIdException("Webhook", id)
        webhookRepository.delete(existing)
    }

    suspend fun list(): List<WebhookResponse> {
        return webhookRepository.findAll()
            .toList()
            .map { it.toResponse(formatter) }
    }

    private fun Webhook.toResponse(dateTimeFormatter: DateTimeFormatter): WebhookResponse {
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
