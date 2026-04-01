package com.manage.crm.webhook.domain.repository

import com.manage.crm.webhook.domain.Webhook
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WebhookRepository :
    CoroutineCrudRepository<Webhook, Long>,
    WebhookCustomRepository {
    suspend fun findByName(name: String): Webhook?
}
