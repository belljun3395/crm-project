package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CampaignTest : BehaviorSpec({
    given("Campaign#allMatchPropertyKeys") {
        `when`("all keys match exactly") {
            val campaign = CampaignFixtures.aCampaign()
                .withProperties(CampaignProperties(listOf(CampaignProperty("key1", "v"), CampaignProperty("key2", "v"))))
                .build()

            then("returns true") {
                campaign.allMatchPropertyKeys(listOf("key1", "key2")) shouldBe true
            }
        }

        `when`("same size but different key name") {
            val campaign = CampaignFixtures.aCampaign()
                .withProperties(CampaignProperties(listOf(CampaignProperty("key1", "v"), CampaignProperty("key2", "v"))))
                .build()

            then("returns false") {
                campaign.allMatchPropertyKeys(listOf("key1", "key3")) shouldBe false
            }
        }

        `when`("input has fewer keys than campaign properties") {
            val campaign = CampaignFixtures.aCampaign()
                .withProperties(CampaignProperties(listOf(CampaignProperty("key1", "v"), CampaignProperty("key2", "v"))))
                .build()

            then("returns false") {
                campaign.allMatchPropertyKeys(listOf("key1")) shouldBe false
            }
        }

        `when`("campaign has no properties") {
            val campaign = CampaignFixtures.aCampaign()
                .withProperties(CampaignProperties(emptyList()))
                .build()

            then("returns false for non-empty input") {
                campaign.allMatchPropertyKeys(listOf("key1")) shouldBe false
            }

            then("returns true for empty input") {
                campaign.allMatchPropertyKeys(emptyList()) shouldBe true
            }
        }

        `when`("input keys are empty") {
            val campaign = CampaignFixtures.aCampaign()
                .withProperties(CampaignProperties(listOf(CampaignProperty("key1", "v"))))
                .build()

            then("returns false") {
                campaign.allMatchPropertyKeys(emptyList()) shouldBe false
            }
        }
    }
})
