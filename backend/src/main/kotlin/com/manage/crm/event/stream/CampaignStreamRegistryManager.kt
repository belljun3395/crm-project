package com.manage.crm.event.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

/**
 * Maintains stream processing registry state in Redis.
 *
 * Registry responsibilities:
 * 1. Track active campaign ids for consumer polling.
 * 2. Persist per-campaign last processed stream id.
 */
@Service
class CampaignStreamRegistryManager(
    private val reactiveStringRedisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger { }

    companion object {
        private const val ACTIVE_CAMPAIGNS_KEY = "campaign:dashboard:active"
        private const val LAST_PROCESSED_KEY_PREFIX = "campaign:dashboard:last-processed"

        /**
         * Builds Redis key that stores stream cursor for one campaign.
         */
        fun getLastProcessedKey(campaignId: Long) = "$LAST_PROCESSED_KEY_PREFIX:$campaignId"
    }

    /**
     * Marks a campaign as active for dashboard stream consumption.
     */
    suspend fun registerCampaign(campaignId: Long) {
        try {
            reactiveStringRedisTemplate.opsForSet()
                .add(ACTIVE_CAMPAIGNS_KEY, campaignId.toString())
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Failed to register campaign: $campaignId" }
        }
    }

    /**
     * Removes campaign from active set and clears its cursor state.
     */
    suspend fun unregisterCampaign(campaignId: Long) {
        try {
            reactiveStringRedisTemplate.opsForSet()
                .remove(ACTIVE_CAMPAIGNS_KEY, campaignId.toString())
                .awaitSingle()
            reactiveStringRedisTemplate.delete(getLastProcessedKey(campaignId)).awaitFirstOrNull()
        } catch (e: Exception) {
            log.error(e) { "Failed to unregister campaign: $campaignId" }
        }
    }

    /**
     * Returns all active campaign ids currently tracked in Redis.
     */
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

    /**
     * Returns last processed stream id for a campaign, if present.
     */
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

    /**
     * Persists latest processed stream id for consumer resume.
     */
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
