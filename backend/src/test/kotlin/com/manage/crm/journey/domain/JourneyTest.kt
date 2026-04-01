package com.manage.crm.journey.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class JourneyTest :
    BehaviorSpec({
        given("Journey#new creates draft-like entity") {
            `when`("creating with inactive flag") {
                val journey =
                    Journey.new(
                        name = "welcome",
                        triggerType = "EVENT",
                        triggerEventName = "SIGNUP",
                        triggerSegmentId = null,
                        triggerSegmentEvent = null,
                        triggerSegmentWatchFields = null,
                        triggerSegmentCountThreshold = null,
                        active = false,
                        lifecycleStatus = "DRAFT",
                        version = 1,
                    )

                then("it keeps provided core fields") {
                    journey.id.shouldBeNull()
                    journey.name shouldBe "welcome"
                    journey.triggerType shouldBe "EVENT"
                    journey.triggerEventName shouldBe "SIGNUP"
                    journey.active shouldBe false
                    journey.lifecycleStatus shouldBe "DRAFT"
                    journey.version shouldBe 1
                }

                then("persistence-managed timestamp remains unset") {
                    journey.createdAt.shouldBeNull()
                }
            }
        }

        given("Journey#new receives boundary values") {
            `when`("version is non-positive") {
                val journey =
                    Journey.new(
                        name = "legacy",
                        triggerType = "EVENT",
                        triggerEventName = null,
                        triggerSegmentId = null,
                        triggerSegmentEvent = null,
                        triggerSegmentWatchFields = null,
                        triggerSegmentCountThreshold = null,
                        active = true,
                        lifecycleStatus = "ACTIVE",
                        version = 0,
                    )

                then("current domain constructor preserves the incoming value") {
                    journey.version shouldBe 0
                }
            }
        }
    })
