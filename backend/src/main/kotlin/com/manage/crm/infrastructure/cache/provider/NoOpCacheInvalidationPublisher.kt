package com.manage.crm.infrastructure.cache.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["cache.invalidation.enabled"],
    havingValue = "false",
    matchIfMissing = false
)
class NoOpCacheInvalidationPublisher : CacheInvalidationPublisher {

    private val log = KotlinLogging.logger {}

    override fun publishCacheInvalidation(keys: List<String>) {
        log.debug { "NoOp cache invalidation for keys: $keys" }
    }
}
