package com.manage.crm.event.stream

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CampaignStreamRegistryManagerTest : BehaviorSpec({
    lateinit var reactiveStringRedisTemplate: ReactiveRedisTemplate<String, String>
    lateinit var setOps: ReactiveSetOperations<String, String>
    lateinit var valueOps: ReactiveValueOperations<String, String>
    lateinit var manager: CampaignStreamRegistryManager

    beforeContainer {
        reactiveStringRedisTemplate = mockk()
        setOps = mockk()
        valueOps = mockk()
        every { reactiveStringRedisTemplate.opsForSet() } returns setOps
        every { reactiveStringRedisTemplate.opsForValue() } returns valueOps
        manager = CampaignStreamRegistryManager(reactiveStringRedisTemplate)
    }

    given("CampaignStreamRegistryManager") {
        `when`("registerCampaign") {
            every { setOps.add(any(), any()) } returns Mono.just(1L)

            manager.registerCampaign(1L)

            then("adds campaign id to active set") {
                verify(exactly = 1) { setOps.add("campaign:dashboard:active", "1") }
            }
        }

        `when`("registerCampaign when Redis fails") {
            every { setOps.add(any(), any()) } returns Mono.error(RuntimeException("Redis down"))

            then("swallows exception without propagating") {
                manager.registerCampaign(2L)
            }
        }

        `when`("unregisterCampaign") {
            every { setOps.remove(any(), any()) } returns Mono.just(1L)
            every { reactiveStringRedisTemplate.delete(any<String>()) } returns Mono.just(1L)

            manager.unregisterCampaign(3L)

            then("removes campaign from active set and deletes cursor key") {
                verify(exactly = 1) { setOps.remove("campaign:dashboard:active", "3") }
                verify(exactly = 1) {
                    reactiveStringRedisTemplate.delete("campaign:dashboard:last-processed:3")
                }
            }
        }

        `when`("getActiveCampaigns returns entries") {
            every { setOps.members("campaign:dashboard:active") } returns Flux.just("10", "20", "30")

            val result = manager.getActiveCampaigns()

            then("returns parsed campaign id set") {
                result shouldBe setOf(10L, 20L, 30L)
            }
        }

        `when`("getActiveCampaigns when Redis fails") {
            every { setOps.members(any()) } returns Flux.error(RuntimeException("Redis down"))

            val result = manager.getActiveCampaigns()

            then("returns empty set") {
                result shouldBe emptySet()
            }
        }

        `when`("getLastProcessedId returns value") {
            every { valueOps.get("campaign:dashboard:last-processed:5") } returns Mono.just("5-99")

            val result = manager.getLastProcessedId(5L)

            then("returns stored stream id") {
                result shouldBe "5-99"
            }
        }

        `when`("getLastProcessedId is absent") {
            every { valueOps.get(any()) } returns Mono.empty()

            val result = manager.getLastProcessedId(6L)

            then("returns null") {
                result shouldBe null
            }
        }

        `when`("updateLastProcessedId") {
            every { valueOps.set(any(), any()) } returns Mono.just(true)

            manager.updateLastProcessedId(7L, "7-42")

            then("persists stream id for campaign") {
                verify(exactly = 1) { valueOps.set("campaign:dashboard:last-processed:7", "7-42") }
            }
        }

        `when`("getLastProcessedKey") {
            then("builds key with correct prefix and campaign id") {
                CampaignStreamRegistryManager.getLastProcessedKey(99L) shouldBe
                    "campaign:dashboard:last-processed:99"
            }
        }
    }
})
