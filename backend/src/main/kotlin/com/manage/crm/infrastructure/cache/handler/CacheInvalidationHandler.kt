package com.manage.crm.infrastructure.cache.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class CacheInvalidationHandler(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    fun handle(messageJson: String, isSnsWrapped: Boolean = false) {
        try {
            val payload = if (isSnsWrapped) {
                val messageMap = objectMapper.readValue(messageJson, Map::class.java) as Map<*, *>
                val snsMessage = messageMap["Message"] as String
                objectMapper.readValue(snsMessage, Map::class.java) as Map<*, *>
            } else {
                objectMapper.readValue(messageJson, Map::class.java) as Map<*, *>
            }

            if (payload["action"] == "invalidate") {
                val keys = payload["keys"] as? List<String>
                if (!keys.isNullOrEmpty()) {
                    log.info { "Invalidating cache keys: $keys" }
                    redisTemplate.delete(keys)
                    log.info { "Successfully invalidated cache keys: $keys" }
                } else {
                    log.warn { "No keys found in invalidation message payload: $payload" }
                }
            } else {
                log.warn { "Unknown action in message payload: $payload" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error processing cache invalidation message: $messageJson" }
        }
    }
}
