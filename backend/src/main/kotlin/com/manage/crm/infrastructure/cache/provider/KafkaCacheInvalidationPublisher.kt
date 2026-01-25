package com.manage.crm.infrastructure.cache.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["message.provider"], havingValue = "kafka")
class KafkaCacheInvalidationPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${cache.invalidation.topic:cache-invalidation}")
    private val topic: String
) : CacheInvalidationPublisher {

    private val log = KotlinLogging.logger {}

    override fun publishCacheInvalidation(keys: List<String>) {
        if (keys.isEmpty()) {
            log.warn { "Skip cache invalidation publish: keys is empty" }
            return
        }

        val message = """{"action":"invalidate", "keys":${keys.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}"""

        runCatching {
            kafkaTemplate.send(topic, message)
        }.onSuccess {
            log.info { "Successfully published cache invalidation to Kafka for keys: $keys" }
        }.onFailure { e ->
            log.error(e) { "Failed to publish cache invalidation to Kafka for keys: $keys" }
        }
    }
}
