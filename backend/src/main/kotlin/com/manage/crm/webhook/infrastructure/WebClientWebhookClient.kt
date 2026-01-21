package com.manage.crm.webhook.infrastructure

import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebClientWebhookClient(
    webClientBuilder: WebClient.Builder
) : WebhookClient {
    private val log = KotlinLogging.logger {}

    private val webClient = webClientBuilder
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(10))
            )
        )
        .build()

    override suspend fun send(webhook: Webhook, payload: WebhookEventPayload) {
        if (!isUrlSafe(webhook.url)) {
            log.warn { "Blocked webhook to unsafe URL: ${webhook.url}" }
            return
        }

        runCatching {
            webClient.post()
                .uri(webhook.url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    throw RuntimeException("Webhook returned error status: ${response.statusCode()}")
                }
                .bodyToMono(String::class.java)
                .awaitSingleOrNull()
        }.onSuccess {
            log.info { "Webhook delivered: id=${webhook.id}, url=${webhook.url}" }
        }.onFailure { error ->
            log.error(error) { "Webhook delivery failed: id=${webhook.id}, url=${webhook.url}" }
        }
    }

    private fun isUrlSafe(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return@runCatching false
            !host.startsWith("localhost") &&
                !host.startsWith("127.") &&
                !host.startsWith("10.") &&
                !host.startsWith("192.168.") &&
                !host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
        }.getOrDefault(false)
    }
}
