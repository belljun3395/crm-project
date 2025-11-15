package com.manage.crm.infrastructure.cache.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

@Primary
@Component
@ConditionalOnBean(SnsClient::class)
class SnsCacheInvalidationPublisher(
    private val snsClient: SnsClient,
    @Value("\${spring.aws.sns.cache-invalidation-topic-arn:#{null}}")
    private val snsTopicArn: String?
) : CacheInvalidationPublisher {

    private val log = KotlinLogging.logger {}

    override fun publishCacheInvalidation(keys: List<String>) {
        if (snsTopicArn.isNullOrBlank()) {
            log.warn { "Skip cache invalidation publish: spring.aws.sns.cache-invalidation-topic-arn is not set" }
            return
        }

        if (keys.isEmpty()) {
            log.warn { "Skip cache invalidation publish: keys is empty" }
            return
        }

        val message = """{"action":"invalidate", "keys":${keys.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}"""

        runCatching {
            val publishRequest = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .message(message)
                .build()
            snsClient.publish(publishRequest)
        }.onSuccess {
            log.info { "Successfully published cache invalidation for keys: $keys" }
        }.onFailure { e ->
            log.error(e) { "Failed to publish cache invalidation for keys: $keys" }
        }
    }
}
