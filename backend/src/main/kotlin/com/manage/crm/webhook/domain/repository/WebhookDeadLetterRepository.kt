package com.manage.crm.webhook.domain.repository

import com.manage.crm.webhook.domain.WebhookDeadLetter
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WebhookDeadLetterRepository : CoroutineCrudRepository<WebhookDeadLetter, Long> {
    fun findByWebhookIdOrderByCreatedAtDesc(webhookId: Long): Flow<WebhookDeadLetter>
}
