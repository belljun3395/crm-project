package com.manage.crm.event.domain.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
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
    lateinit var campaignCacheManager: CampaignCacheManager

    beforeContainer {
        redisTemplate = mockk()
        hashOperations = mockk()
        valueOperations = mockk()
        mapper = mockk()

        every { redisTemplate.opsForHash<String, Any>() } returns hashOperations
        every { redisTemplate.opsForValue() } returns valueOperations

        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

        campaignCacheManager = CampaignCacheManager(
            redisTemplate = redisTemplate,
            mapper = mapper,
            objectMapper = objectMapper
        )
    }

    given("CampaignCacheManager#toHash and #loadHash") {
        `when`("campaign has custom properties") {
            val campaign = CampaignFixtures.aCampaign()
                .withId(1L)
                .withName("campaign-a")
                .withCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .withProperties(
                    CampaignProperties(
                        listOf(
                            CampaignProperty("channel", "email"),
                            CampaignProperty("source", "seed-script")
                        )
                    )
                )
                .build()

            every { mapper.toHash(campaign) } returns mutableMapOf<String, Any>(
                "id" to campaign.id!!,
                "name" to campaign.name,
                "createdAt" to campaign.createdAt!!
            )

            val hash = campaignCacheManager.toHash(campaign)
            val reloaded = campaignCacheManager.loadHash(hash)

            then("flattens properties with stable key prefix") {
                hash shouldContain ("properties::channel" to "email")
                hash shouldContain ("properties::source" to "seed-script")
            }

            then("restores campaign properties from flattened hash") {
                reloaded.properties.value.map { it.key to it.value }.toSet() shouldBe setOf(
                    "channel" to "email",
                    "source" to "seed-script"
                )
            }
        }
    }

    given("CampaignCacheManager#save") {
        `when`("saving a campaign") {
            val campaign = CampaignFixtures.aCampaign()
                .withId(11L)
                .withName("campaign-cache")
                .withCreatedAt(LocalDateTime.of(2026, 3, 1, 12, 0))
                .withProperties(PropertiesFixtures.giveMeOne().buildCampaign())
                .build()
            val hashCapture = slot<Map<String, Any>>()

            every { mapper.toHash(campaign) } returns mutableMapOf<String, Any>(
                "id" to campaign.id!!,
                "name" to campaign.name,
                "createdAt" to campaign.createdAt!!
            )
            every { redisTemplate.delete("campaign::${campaign.id}") } returns Mono.just(1L)
            every { hashOperations.putAll("campaign::${campaign.id}", capture(hashCapture)) } returns Mono.just(true)
            every {
                valueOperations.set("campaign::name::${campaign.name}", campaign.id!!)
            } returns Mono.just(true)

            campaignCacheManager.save(campaign)

            then("deletes old hash before writing new hash") {
                verifyOrder {
                    redisTemplate.delete("campaign::${campaign.id}")
                    hashOperations.putAll("campaign::${campaign.id}", any())
                }
            }

            then("writes both flattened properties and name lookup") {
                hashCapture.isCaptured shouldBe true
                hashCapture.captured.keys.any { it.startsWith("properties::") } shouldBe true
            }
        }
    }
})
