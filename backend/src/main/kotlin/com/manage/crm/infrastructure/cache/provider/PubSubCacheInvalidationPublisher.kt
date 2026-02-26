package com.manage.crm.infrastructure.cache.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.spring.pubsub.core.PubSubTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "gcp")
class PubSubCacheInvalidationPublisher(
    private val pubSubTemplate: PubSubTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${gcp.pubsub.cache-invalidation-topic}") private val topicId: String
) : CacheInvalidationPublisher {

    private val log = KotlinLogging.logger {}

    override fun publishCacheInvalidation(keys: List<String>) {
        if (keys.isEmpty()) {
            log.warn { "Skip cache invalidation publish: keys is empty" }
            return
        }

        val message = objectMapper.writeValueAsString(mapOf("action" to "invalidate", "keys" to keys))

        runCatching {
            pubSubTemplate.publish(topicId, message)
        }.onSuccess {
            log.info { "Successfully published cache invalidation to Pub/Sub for keys: $keys" }
        }.onFailure { e ->
            log.error(e) { "Failed to publish cache invalidation to Pub/Sub for keys: $keys" }
        }
    }
}
