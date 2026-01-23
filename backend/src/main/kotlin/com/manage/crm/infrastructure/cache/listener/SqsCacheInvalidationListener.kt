package com.manage.crm.infrastructure.cache.listener

import com.manage.crm.infrastructure.cache.handler.CacheInvalidationHandler
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
class SqsCacheInvalidationListener(
    private val handler: CacheInvalidationHandler
) {
    private val log = KotlinLogging.logger {}

    @SqsListener(value = ["crm-dr-cache-invalidation-queue-aws"])
    fun listenAwsQueue(messageJson: String) {
        log.info { "Received message from AWS SQS (AWS Queue): $messageJson" }
        handler.handle(messageJson, isSnsWrapped = true)
    }

    @SqsListener(value = ["crm-dr-cache-invalidation-queue-gcp"])
    fun listenGcpQueue(messageJson: String) {
        log.info { "Received message from AWS SQS (GCP Queue): $messageJson" }
        handler.handle(messageJson, isSnsWrapped = true)
    }
}
