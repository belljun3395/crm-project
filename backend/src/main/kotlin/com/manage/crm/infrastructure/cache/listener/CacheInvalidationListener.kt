package com.manage.crm.infrastructure.cache.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class CacheInvalidationListener(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // AWS EKS 앱을 위한 SQS 큐 리스너
    @SqsListener(
        value = ["crm-dr-cache-invalidation-queue-aws"] // SQS 큐 이름
    )
    fun listenAwsQueue(messageJson: String) {
        log.info("Received message from AWS SQS: {}", messageJson)
        processMessage(messageJson)
    }

    // GCP GKE 앱을 위한 SQS 큐 리스너 (GCP 앱도 AWS SQS를 리스닝)
    @SqsListener(
        value = ["crm-dr-cache-invalidation-queue-gcp"] // SQS 큐 이름
    )
    fun listenGcpQueue(messageJson: String) {
        log.info("Received message from GCP SQS: {}", messageJson)
        processMessage(messageJson)
    }

    private fun processMessage(messageJson: String) {
        try {
            val messageMap = objectMapper.readValue(messageJson, Map::class.java) as Map<*, *>
            val snsMessage = messageMap["Message"] as String // SNS 메시지 구조에서 실제 메시지 추출
            val payload = objectMapper.readValue(snsMessage, Map::class.java) as Map<*, *>

            if (payload["action"] == "invalidate") {
                val keys = payload["keys"] as? List<String>
                if (keys != null && keys.isNotEmpty()) {
                    log.info("Invalidating cache keys: {}", keys)
                    redisTemplate.delete(keys)
                    log.info("Successfully invalidated cache keys: {}", keys)
                } else {
                    log.warn("No keys found in invalidation message payload: {}", payload)
                }
            } else {
                log.warn("Unknown action in message payload: {}", payload)
            }
        } catch (e: Exception) {
            log.error("Error processing cache invalidation message: {}", messageJson, e)
        }
    }
}
