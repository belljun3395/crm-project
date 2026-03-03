package com.manage.crm.webhook.application

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchIn
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.webhook.WebhookDeliveryResult
import com.manage.crm.webhook.WebhookDeliveryStatus
import com.manage.crm.webhook.domain.Webhook
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Service
class WebhookFailureAlertService(
    private val actionDispatchService: ActionDispatchService,
    @Value("\${webhook.alert.enabled:false}")
    private val enabled: Boolean,
    @Value("\${webhook.alert.failure-threshold:3}")
    private val failureThreshold: Int,
    @Value("\${webhook.alert.cooldown-seconds:300}")
    private val cooldownSeconds: Long,
    @Value("\${webhook.alert.channel:SLACK}")
    private val alertChannel: String,
    @Value("\${webhook.alert.destination:}")
    private val alertDestination: String
) {
    private val log = KotlinLogging.logger {}
    private val failureCountByWebhook = ConcurrentHashMap<Long, AtomicInteger>()
    private val lastAlertAtByWebhook = ConcurrentHashMap<Long, Long>()

    suspend fun onDeliveryResult(webhook: Webhook, result: WebhookDeliveryResult) {
        if (!enabled) {
            return
        }

        val webhookId = webhook.id ?: return
        if (result.status == WebhookDeliveryStatus.SUCCESS) {
            failureCountByWebhook.remove(webhookId)
            return
        }

        val threshold = failureThreshold.coerceAtLeast(1)
        val currentFailures = failureCountByWebhook
            .computeIfAbsent(webhookId) { AtomicInteger(0) }
            .incrementAndGet()

        if (currentFailures < threshold) {
            return
        }
        if (alertDestination.isBlank()) {
            log.warn { "Webhook alert destination is empty. Skip alert for webhookId=$webhookId" }
            return
        }

        val nowEpochMillis = System.currentTimeMillis()
        val lastAlertEpochMillis = lastAlertAtByWebhook[webhookId] ?: 0L
        val cooldownMillis = cooldownSeconds.coerceAtLeast(0L) * 1000L
        if (nowEpochMillis - lastAlertEpochMillis < cooldownMillis) {
            return
        }

        val channel = runCatching { ActionChannel.from(alertChannel) }
            .getOrElse {
                log.warn { "Unsupported webhook.alert.channel=$alertChannel. fallback=SLACK" }
                ActionChannel.SLACK
            }
        val message = buildString {
            append("[Webhook Alert] delivery failures reached threshold")
            append(" webhookId=").append(webhookId)
            append(", name=").append(webhook.name)
            append(", url=").append(webhook.url)
            append(", failures=").append(currentFailures)
            append(", latestStatus=").append(result.status.name)
            result.errorMessage?.let { append(", error=").append(it) }
        }

        val dispatchResult = runCatching {
            actionDispatchService.dispatch(
                ActionDispatchIn(
                    channel = channel,
                    destination = alertDestination,
                    subject = "Webhook delivery alert",
                    body = message,
                    variables = emptyMap(),
                    campaignId = null,
                    journeyExecutionId = null
                )
            )
        }.onFailure { error ->
            log.error(error) { "Failed to send webhook alert for webhookId=$webhookId" }
        }.getOrNull()

        if (dispatchResult?.status != ActionDispatchStatus.SUCCESS) {
            if (dispatchResult != null) {
                log.warn { "Webhook alert dispatch failed for webhookId=$webhookId, status=${dispatchResult.status}" }
            }
            return
        }

        lastAlertAtByWebhook[webhookId] = nowEpochMillis
        failureCountByWebhook[webhookId]?.set(0)
    }
}
