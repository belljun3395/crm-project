package com.manage.crm.event.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CampaignEventsTest :
    BehaviorSpec({
        given("CampaignEvents#new") {
            `when`("creating relation with campaignId and eventId") {
                val relation =
                    CampaignEvents.new(
                        campaignId = 11L,
                        eventId = 101L,
                    )

                then("initializes relation ids") {
                    relation.campaignId shouldBe 11L
                    relation.eventId shouldBe 101L
                }

                then("leaves persistence-managed fields unset") {
                    relation.id.shouldBeNull()
                    relation.createdAt.shouldBeNull()
                }
            }
        }
    })
