package com.manage.crm.webhook.domain.repository

import com.manage.crm.webhook.domain.Webhook

interface WebhookCustomRepository {
    suspend fun findActiveByEvent(eventType: String): List<Webhook>
}
