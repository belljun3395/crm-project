package com.manage.crm.infrastructure.cache.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.spring.pubsub.core.PubSubTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * GCP Pub/Sub 기반 캐시 무효화 구독자.
 *
 * cloud.provider=gcp 환경에서만 활성화된다.
 * AWS 환경의 SNS-SQS 조합 대신 GCP Pub/Sub 구독을 통해 캐시 무효화 이벤트를 수신한다.
 * SNS 래핑 없이 직접 JSON 페이로드를 처리한다.
 */
@Profile("!test & !openapi")
@Component
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "gcp")
class PubSubCacheInvalidationSubscriber(
    private val pubSubTemplate: PubSubTemplate,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    @Value("\${gcp.pubsub.cache-invalidation-subscription}") private val subscriptionId: String
) {

    private val log = KotlinLogging.logger {}
    private var subscriber: com.google.cloud.pubsub.v1.Subscriber? = null

    @PostConstruct
    fun startSubscription() {
        log.info { "Starting Pub/Sub cache invalidation subscriber for subscription: $subscriptionId" }
        subscriber = pubSubTemplate.subscribe(subscriptionId) { message ->
            val data = message.pubsubMessage.data.toStringUtf8()
            log.info { "Received cache invalidation message from Pub/Sub: $data" }
            processMessage(data)
            message.ack()
        }
    }

    @PreDestroy
    fun stopSubscription() {
        subscriber?.stopAsync()
        log.info { "Stopped Pub/Sub cache invalidation subscriber" }
    }

    private fun processMessage(data: String) {
        try {
            val payload = objectMapper.readValue(data, Map::class.java) as Map<*, *>

            if (payload["action"] == "invalidate") {
                @Suppress("UNCHECKED_CAST")
                val keys = (payload["keys"] as? List<*>)?.filterIsInstance<String>()
                if (!keys.isNullOrEmpty()) {
                    log.info { "Invalidating cache keys: $keys" }
                    redisTemplate.delete(keys)
                    log.info { "Successfully invalidated cache keys: $keys" }
                } else {
                    log.warn { "No keys found in Pub/Sub invalidation message payload: $payload" }
                }
            } else {
                log.warn { "Unknown action in Pub/Sub message payload: $payload" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error processing cache invalidation message from Pub/Sub: $data" }
        }
    }
}
