package com.manage.crm.event.domain.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.hash.Jackson2HashMapper
import org.springframework.stereotype.Service

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

    fun toHash(campaign: Campaign): Map<String, Any> {
        return mapper.toHash(campaign).also {
            campaign.properties.value.forEach { property ->
                it["${CAMPAIGN_PROPERTIES_KEY_PREFIX}${property.key}"] = property.value
            }
        }
    }

    fun loadHash(hash: Map<String, Any>): Campaign {
        val properties = hash.filterKeys { it.startsWith(CAMPAIGN_PROPERTIES_KEY_PREFIX) }
            .mapKeys { it.key.removePrefix(CAMPAIGN_PROPERTIES_KEY_PREFIX) }
            .mapValues { it.value.toString() }
        val campaignPropertiesMap = properties.map { (key, value) ->
            Property(key = key, value = value)
        }
        return objectMapper.convertValue(hash, Campaign::class.java).also {
            it.properties = Properties(campaignPropertiesMap)
        }
    }

    suspend fun save(campaign: Campaign): Campaign {
        saveWithId(campaign)
        saveWithName(campaign)
        log.debug { "Successfully saved campaign as campaign:${campaign.id}" }
        return campaign
    }

    private suspend fun saveWithId(campaign: Campaign) {
        redisTemplate.opsForHash<String, Any>()
            .putAll("${CAMPAIGN_CACHE_KEY_PREFIX}${campaign.id}", toHash(campaign))
            .awaitSingle()
    }

    private suspend fun saveWithName(campaign: Campaign) {
        redisTemplate.opsForValue()
            .set("${CAMPAIGN_CACHE_KEY_PREFIX}${Campaign.UNIQUE_FIELDS.NAME}${SPLIT}${campaign.name}", campaign.id!!)
            .awaitSingle()
    }

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

    suspend fun loadAndSaveIfMiss(id: Long, findForLoad: suspend () -> Campaign?): Campaign? {
        return load(id) ?: run {
            findForLoad.invoke()
                ?.also { save(it) }
        }
    }

    suspend fun load(uniqKey: String, value: String): Campaign? {
        return redisTemplate.opsForValue().get("${CAMPAIGN_CACHE_KEY_PREFIX}$uniqKey${SPLIT}$value")
            .awaitSingleOrNull()
            ?.toString()
            ?.toLong()
            ?.let { load(it) }
    }

    suspend fun loadAndSaveIfMiss(uniqKey: String, value: String, findForLoad: suspend () -> Campaign?): Campaign? {
        return load(uniqKey, value) ?: run {
            findForLoad.invoke()
                ?.also { save(it) }
        }
    }
}
