package com.manage.crm.event.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CampaignSegmentsTest :
    BehaviorSpec({
        given("CampaignSegments#new") {
            `when`("creating relation with campaignId and segmentId") {
                val relation =
                    CampaignSegments.new(
                        campaignId = 21L,
                        segmentId = 301L,
                    )

                then("initializes relation ids") {
                    relation.campaignId shouldBe 21L
                    relation.segmentId shouldBe 301L
                }

                then("leaves persistence-managed fields unset") {
                    relation.id.shouldBeNull()
                    relation.createdAt.shouldBeNull()
                }
            }
        }
    })
