package com.manage.crm.webhook.infrastructure

import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.MediaType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebClientWebhookClient : WebhookClient {
    private val log = KotlinLogging.logger {}
    private val webClient = WebClient.builder().build()

    override suspend fun send(webhook: Webhook, payload: WebhookEventPayload) {
        runCatching {
            webClient.post()
                .uri(webhook.url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingleOrNull()
        }.onSuccess {
            log.info { "Webhook delivered: id=${webhook.id}, url=${webhook.url}" }
        }.onFailure { error ->
            log.error(error) { "Webhook delivery failed: id=${webhook.id}, url=${webhook.url}" }
        }
    }
}
