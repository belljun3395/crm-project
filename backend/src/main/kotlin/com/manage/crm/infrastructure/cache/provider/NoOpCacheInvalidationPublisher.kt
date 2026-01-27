package com.manage.crm.infrastructure.cache.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.SnsClient

@Component
@Profile("local || test")
@ConditionalOnMissingBean(SnsClient::class)
@ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
class NoOpCacheInvalidationPublisher : CacheInvalidationPublisher {

    private val log = KotlinLogging.logger {}

    override fun publishCacheInvalidation(keys: List<String>) {
        log.debug { "NoOp cache invalidation for keys: $keys (Test environment)" }
    }
}
