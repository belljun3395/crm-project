package com.manage.crm.infrastructure.cache.listener

import com.manage.crm.infrastructure.cache.handler.CacheInvalidationHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["message.provider"], havingValue = "kafka", matchIfMissing = false)
class KafkaCacheInvalidationListener(
    private val handler: CacheInvalidationHandler
) {
    private val log = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["cache-invalidation"],
        groupId = "crm-cache-invalidation-group"
    )
    fun listen(message: String, acknowledgment: Acknowledgment) {
        log.info { "Received cache invalidation from Kafka: $message" }
        handler.handle(message, isSnsWrapped = false)
        acknowledgment.acknowledge()
    }
}
