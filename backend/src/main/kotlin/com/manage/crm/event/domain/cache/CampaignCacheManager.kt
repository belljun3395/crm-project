package com.manage.crm.event.domain.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.hash.Jackson2HashMapper
import org.springframework.stereotype.Service

/**
 * Handles campaign cache read/write/eviction in Redis.
 *
 * Cache key strategy:
 * 1. `campaign::<id>` stores hash payload.
 * 2. `campaign::name::<name>` stores id lookup for unique name access.
 */
@Service
class CampaignCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val mapper: Jackson2HashMapper,
    private val objectMapper: ObjectMapper
) {
    val log = KotlinLogging.logger {}
    companion object {
        private const val SPLIT = "::"
        private const val CAMPAIGN_CACHE_KEY = "campaign"
        const val CAMPAIGN_CACHE_KEY_PREFIX = CAMPAIGN_CACHE_KEY + SPLIT
        private const val CAMPAIGN_PROPERTIES_KEY = "properties"
        const val CAMPAIGN_PROPERTIES_KEY_PREFIX = CAMPAIGN_PROPERTIES_KEY + SPLIT
    }

    /**
     * Converts campaign aggregate to Redis hash payload with flattened property keys.
     */
    fun toHash(campaign: Campaign): Map<String, Any> {
        return mapper.toHash(campaign).also {
            campaign.properties.value.forEach { property ->
                it["${CAMPAIGN_PROPERTIES_KEY_PREFIX}${property.key}"] = property.value
            }
        }
    }

    /**
     * Restores campaign aggregate from Redis hash payload.
     */
    fun loadHash(hash: Map<String, Any>): Campaign {
        val properties = hash.filterKeys { it.startsWith(CAMPAIGN_PROPERTIES_KEY_PREFIX) }
            .mapKeys { it.key.removePrefix(CAMPAIGN_PROPERTIES_KEY_PREFIX) }
            .mapValues { it.value.toString() }
        val campaignPropertiesMap = properties.map { (key, value) ->
            CampaignProperty(key = key, value = value)
        }
        return objectMapper.convertValue(hash, Campaign::class.java).also {
            it.properties = CampaignProperties(campaignPropertiesMap)
        }
    }

    /**
     * Writes campaign cache by id and unique-name lookup key.
     */
    suspend fun save(campaign: Campaign): Campaign {
        saveWithId(campaign)
        saveWithName(campaign)
        log.debug { "Successfully saved campaign as campaign:${campaign.id}" }
        return campaign
    }

    /**
     * Persists full campaign hash under `campaign::<id>`.
     */
    private suspend fun saveWithId(campaign: Campaign) {
        redisTemplate.opsForHash<String, Any>()
            .putAll("${CAMPAIGN_CACHE_KEY_PREFIX}${campaign.id}", toHash(campaign))
            .awaitSingle()
    }

    /**
     * Persists unique name lookup key pointing to campaign id.
     */
    private suspend fun saveWithName(campaign: Campaign) {
        redisTemplate.opsForValue()
            .set("${CAMPAIGN_CACHE_KEY_PREFIX}${Campaign.UNIQUE_FIELDS.NAME}${SPLIT}${campaign.name}", campaign.id!!)
            .awaitSingle()
    }

    /**
     * Loads campaign hash by campaign id and hydrates aggregate.
     */
    suspend fun load(id: Long): Campaign? {
        val hash = mutableMapOf<String, Any>()
        val keys = redisTemplate.opsForHash<String, Any>().keys("${CAMPAIGN_CACHE_KEY_PREFIX}$id").collectList().awaitSingle()
        for (prop in keys) {
            redisTemplate.opsForHash<String, Any>()
                .get("${CAMPAIGN_CACHE_KEY_PREFIX}$id", prop)
                .awaitSingleOrNull()
                ?.let { hash[prop] = it }
        }
        return if (hash.isNotEmpty()) {
            loadHash(hash)
        } else {
            log.warn { "Campaign with id:$id not found in cache" }
            null
        }
    }

    /**
     * Reads campaign by id from cache and falls back to source loader on miss.
     */
    suspend fun loadAndSaveIfMiss(id: Long, findForLoad: suspend () -> Campaign?): Campaign? {
        return load(id) ?: run {
            findForLoad.invoke()
                ?.also { save(it) }
        }
    }

    /**
     * Resolves campaign by unique field value using value-key indirection.
     */
    suspend fun load(uniqKey: String, value: String): Campaign? {
        return redisTemplate.opsForValue().get("${CAMPAIGN_CACHE_KEY_PREFIX}$uniqKey${SPLIT}$value")
            .awaitSingleOrNull()
            ?.toString()
            ?.toLong()
            ?.let { load(it) }
    }

    /**
     * Reads campaign by unique field from cache and fills cache on miss.
     */
    suspend fun loadAndSaveIfMiss(uniqKey: String, value: String, findForLoad: suspend () -> Campaign?): Campaign? {
        return load(uniqKey, value) ?: run {
            findForLoad.invoke()
                ?.also { save(it) }
        }
    }

    /**
     * Removes both id-hash key and unique-name key for campaign cache.
     */
    suspend fun evict(campaignId: Long, campaignName: String) {
        redisTemplate.delete("${CAMPAIGN_CACHE_KEY_PREFIX}$campaignId").awaitSingleOrNull()
        redisTemplate.delete("${CAMPAIGN_CACHE_KEY_PREFIX}${Campaign.UNIQUE_FIELDS.NAME}${SPLIT}$campaignName").awaitSingleOrNull()
    }
}
