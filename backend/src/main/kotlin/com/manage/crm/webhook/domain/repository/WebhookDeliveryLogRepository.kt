package com.manage.crm.webhook.domain.repository

import com.manage.crm.webhook.domain.WebhookDeliveryLog
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WebhookDeliveryLogRepository : CoroutineCrudRepository<WebhookDeliveryLog, Long> {
    fun findByWebhookIdOrderByDeliveredAtDesc(webhookId: Long): Flow<WebhookDeliveryLog>
}
