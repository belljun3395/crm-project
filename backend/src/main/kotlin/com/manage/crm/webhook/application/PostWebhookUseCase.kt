package com.manage.crm.webhook.application

import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseOut
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true")
class PostWebhookUseCase(
    private val webhookRepository: WebhookRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @Transactional
    suspend fun execute(useCaseIn: PostWebhookUseCaseIn): PostWebhookUseCaseOut {
        val id = useCaseIn.id
        val name = useCaseIn.name
        val url = useCaseIn.url
        val events = WebhookEvents.fromValues(useCaseIn.events)
        val active = useCaseIn.active ?: true

        val existing = if (id != null) {
            webhookRepository.findById(id) ?: throw NotFoundByIdException("Webhook", id)
        } else {
            if (webhookRepository.findByName(name) != null) {
                throw AlreadyExistsException("Webhook", "name", name)
            }
            null
        }

        val saved = try {
            if (existing != null) {
                existing.name = name
                existing.url = url
                existing.events = events
                existing.active = active
                webhookRepository.save(existing)
            } else {
                webhookRepository.save(
                    Webhook.new(
                        name = name,
                        url = url,
                        events = events,
                        active = active
                    )
                )
            }
        } catch (e: DataIntegrityViolationException) {
            throw AlreadyExistsException("Webhook", "name", name)
        }

        return out {
            PostWebhookUseCaseOut(
                id = saved.id!!,
                name = saved.name,
                url = saved.url,
                events = saved.events.toValues(),
                active = saved.active,
                createdAt = saved.createdAt?.format(formatter)
            )
        }
    }
}
