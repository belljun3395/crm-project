package com.manage.crm.event.domain.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.hash.Jackson2HashMapper
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class CampaignCacheManagerTest : BehaviorSpec({
    lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>
    lateinit var hashOperations: ReactiveHashOperations<String, String, Any>
    lateinit var valueOperations: ReactiveValueOperations<String, Any>
    lateinit var mapper: Jackson2HashMapper
    lateinit var objectMapper: ObjectMapper
    lateinit var campaignCacheManager: CampaignCacheManager

    beforeContainer {
        redisTemplate = mockk()
        hashOperations = mockk()
        valueOperations = mockk()
        mapper = mockk()
        objectMapper = mockk(relaxed = true)

        every { redisTemplate.opsForHash<String, Any>() } returns hashOperations
        every { redisTemplate.opsForValue() } returns valueOperations

        campaignCacheManager = CampaignCacheManager(
            redisTemplate = redisTemplate,
            mapper = mapper,
            objectMapper = objectMapper
        )
    }

    given("CampaignCacheManager.save") {
        `when`("saving cache for campaign id key") {
            val campaign = Campaign.new(
                id = 1L,
                name = "campaign-a",
                properties = CampaignProperties(
                    listOf(
                        CampaignProperty(key = "channel", value = "email"),
                        CampaignProperty(key = "source", value = "seed-script")
                    )
                ),
                createdAt = LocalDateTime.of(2026, 3, 27, 0, 0, 0)
            )

            val hashCapture = slot<Map<String, Any>>()

            every { mapper.toHash(campaign) } returns mutableMapOf<String, Any>(
                "id" to campaign.id!!,
                "name" to campaign.name,
                "createdAt" to campaign.createdAt!!
            )

            every { redisTemplate.delete("campaign::${campaign.id}") } returns Mono.just(1L)
            every { hashOperations.putAll("campaign::${campaign.id}", capture(hashCapture)) } returns Mono.just(true)
            every { valueOperations.set("campaign::name::${campaign.name}", campaign.id!!) } returns Mono.just(true)

            campaignCacheManager.save(campaign)

            then("delete existing hash before writing new hash fields") {
                verifyOrder {
                    redisTemplate.delete("campaign::${campaign.id}")
                    hashOperations.putAll("campaign::${campaign.id}", any())
                }
            }

            then("write flattened property fields for stable cache hydration") {
                hashCapture.captured shouldContainKey "properties::channel"
                hashCapture.captured shouldContainKey "properties::source"
            }
        }
    }
})
