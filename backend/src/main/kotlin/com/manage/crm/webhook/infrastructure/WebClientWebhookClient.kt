package com.manage.crm.webhook.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.WebhookDeliveryResult
import com.manage.crm.webhook.WebhookDeliveryStatus
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator
import io.netty.channel.ChannelOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebClientWebhookClient(
    webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val rateLimiterRegistry: RateLimiterRegistry,
    @Value("\${webhook.delivery.max-attempts:3}")
    private val maxAttempts: Int,
    @Value("\${webhook.delivery.initial-backoff-millis:200}")
    private val initialBackoffMillis: Long,
    @Value("\${webhook.resilience.circuit-breaker.instance-name:webhookDelivery}")
    private val circuitBreakerInstanceName: String,
    @Value("\${webhook.resilience.rate-limiter.instance-name:webhookDelivery}")
    private val rateLimiterInstanceName: String,
    @Value("\${webhook.signing.enabled:false}")
    private val signingEnabled: Boolean,
    @Value("\${webhook.signing.secret:}")
    private val signingSecret: String
) : WebhookClient {
    private val log = KotlinLogging.logger {}
    private val circuitBreaker by lazy {
        circuitBreakerRegistry.circuitBreaker(circuitBreakerInstanceName)
    }
    private val rateLimiter by lazy {
        rateLimiterRegistry.rateLimiter(rateLimiterInstanceName)
    }

    private val webClient = webClientBuilder
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(10))
            )
        )
        .build()

    private class WebhookHttpStatusException(
        val statusCode: Int,
        message: String
    ) : RuntimeException(message)

    override suspend fun send(webhook: Webhook, payload: WebhookEventPayload): WebhookDeliveryResult {
        if (!isUrlSafe(webhook.url)) {
            log.warn { "Blocked webhook to unsafe URL: ${webhook.url}" }
            return WebhookDeliveryResult(
                status = WebhookDeliveryStatus.BLOCKED,
                attemptCount = 0,
                errorMessage = "Blocked webhook to unsafe URL"
            )
        }

        val attempts = maxAttempts.coerceAtLeast(1)
        val payloadJson = runCatching { objectMapper.writeValueAsString(payload) }
            .getOrElse { error ->
                log.error(error) { "Failed to serialize webhook payload: id=${webhook.id}, url=${webhook.url}" }
                return WebhookDeliveryResult(
                    status = WebhookDeliveryStatus.FAILED,
                    attemptCount = 0,
                    errorMessage = error.message
                )
            }

        var finalResult = WebhookDeliveryResult(
            status = WebhookDeliveryStatus.FAILED,
            attemptCount = attempts,
            errorMessage = "Webhook delivery failed"
        )

        for (attempt in 1..attempts) {
            val result = sendOnce(webhook, payloadJson, attempt)
            if (result.status == WebhookDeliveryStatus.SUCCESS || result.status == WebhookDeliveryStatus.BLOCKED) {
                return result
            }
            finalResult = result

            if (attempt < attempts) {
                val backoffMillis = initialBackoffMillis * (1L shl (attempt - 1))
                delay(backoffMillis)
            }
        }

        return finalResult
    }

    private suspend fun sendOnce(
        webhook: Webhook,
        payloadJson: String,
        attempt: Int
    ): WebhookDeliveryResult {
        return runCatching {
            val timestamp = OffsetDateTime.now().toString()
            val nonce = UUID.randomUUID().toString()
            val signature = createSignature(timestamp, nonce, payloadJson)

            val responseStatus = webClient.post()
                .uri(webhook.url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { headers ->
                    if (signingEnabled && signature != null) {
                        headers.add("X-Webhook-Timestamp", timestamp)
                        headers.add("X-Webhook-Nonce", nonce)
                        headers.add("X-Webhook-Signature", signature)
                    }
                }
                .bodyValue(payloadJson)
                .exchangeToMono { response ->
                    response.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .flatMap {
                            val status = response.statusCode().value()
                            if (status in 200..299) {
                                Mono.just(status)
                            } else {
                                Mono.error(
                                    WebhookHttpStatusException(
                                        statusCode = status,
                                        message = "Webhook returned non-success status: $status"
                                    )
                                )
                            }
                        }
                }
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .awaitSingleOrNull()
                ?: 0

            log.info { "Webhook delivered: id=${webhook.id}, url=${webhook.url}, attempt=$attempt" }
            WebhookDeliveryResult(
                status = WebhookDeliveryStatus.SUCCESS,
                attemptCount = attempt,
                responseStatus = responseStatus
            )
        }.getOrElse { error ->
            when (error) {
                is CallNotPermittedException, is RequestNotPermitted -> {
                    log.warn(error) {
                        "Webhook delivery blocked by resilience policy: id=${webhook.id}, url=${webhook.url}, attempt=$attempt"
                    }
                    WebhookDeliveryResult(
                        status = WebhookDeliveryStatus.BLOCKED,
                        attemptCount = attempt,
                        errorMessage = error.message
                    )
                }
                is WebhookHttpStatusException -> {
                    log.warn {
                        "Webhook returned non-success status: id=${webhook.id}, status=${error.statusCode}, attempt=$attempt"
                    }
                    WebhookDeliveryResult(
                        status = WebhookDeliveryStatus.FAILED,
                        attemptCount = attempt,
                        responseStatus = error.statusCode,
                        errorMessage = error.message
                    )
                }
                else -> {
                    log.error(error) { "Webhook delivery failed: id=${webhook.id}, url=${webhook.url}, attempt=$attempt" }
                    WebhookDeliveryResult(
                        status = WebhookDeliveryStatus.FAILED,
                        attemptCount = attempt,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    private fun createSignature(timestamp: String, nonce: String, body: String): String? {
        if (!signingEnabled || signingSecret.isBlank()) {
            return null
        }
        val payload = "$timestamp.$nonce.$body"
        val secretKeySpec = SecretKeySpec(signingSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(secretKeySpec) }
        val digest = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "sha256=$hex"
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
