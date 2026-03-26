package com.manage.crm.event.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
class CampaignStreamRegistryService(
    private val reactiveStringRedisTemplate: ReactiveRedisTemplate<String, String>
) {
    val log = KotlinLogging.logger { }

    companion object {
        private const val ACTIVE_CAMPAIGNS_KEY = "campaign:dashboard:active"
        private const val LAST_PROCESSED_KEY_PREFIX = "campaign:dashboard:last-processed"

        fun getLastProcessedKey(campaignId: Long) = "$LAST_PROCESSED_KEY_PREFIX:$campaignId"
    }

    suspend fun registerCampaign(campaignId: Long) {
        try {
            reactiveStringRedisTemplate.opsForSet()
                .add(ACTIVE_CAMPAIGNS_KEY, campaignId.toString())
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Failed to register campaign: $campaignId" }
        }
    }

    suspend fun getActiveCampaigns(): Set<Long> {
        return try {
            reactiveStringRedisTemplate.opsForSet()
                .members(ACTIVE_CAMPAIGNS_KEY)
                .collectList()
                .awaitSingle()
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        } catch (e: Exception) {
            log.error(e) { "Failed to get active campaigns" }
            emptySet()
        }
    }

    suspend fun getLastProcessedId(campaignId: Long): String? {
        return try {
            reactiveStringRedisTemplate.opsForValue()
                .get(getLastProcessedKey(campaignId))
                .awaitFirstOrNull()
        } catch (e: Exception) {
            log.error(e) { "Failed to get last processed ID for campaign: $campaignId" }
            null
        }
    }

    suspend fun updateLastProcessedId(campaignId: Long, streamId: String) {
        try {
            reactiveStringRedisTemplate.opsForValue()
                .set(getLastProcessedKey(campaignId), streamId)
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Failed to update last processed ID for campaign: $campaignId" }
        }
    }
}
