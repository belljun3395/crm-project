package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class CampaignTest : FeatureSpec({
    feature("Campaign#allMatchPropertyKeys") {
        scenario("all match property keys") {
            // given
            val campaign = Campaign.new(
                name = "testCampaign",
                properties = CampaignProperties(
                    value = listOf(
                        CampaignProperty("key1", "value1"),
                        CampaignProperty("key2", "value2")
                    )
                )
            )

            // when
            val result = campaign.allMatchPropertyKeys(listOf("key1", "key2"))

            // then
            result shouldBe true
        }

        scenario("not all match property keys - same size") {
            // given
            val campaign = Campaign.new(
                name = "testCampaign",
                properties = CampaignProperties(
                    value = listOf(
                        CampaignProperty("key1", "value1"),
                        CampaignProperty("key2", "value2")
                    )
                )
            )

            // when
            val result = campaign.allMatchPropertyKeys(listOf("key1", "key3"))

            // then
            result shouldBe false
        }

        scenario("not all match property keys - different size") {
            // given
            val campaign = Campaign.new(
                name = "testCampaign",
                properties = CampaignProperties(
                    value = listOf(
                        CampaignProperty("key1", "value1"),
                        CampaignProperty("key2", "value2")
                    )
                )
            )

            // when
            val result = campaign.allMatchPropertyKeys(listOf("key1"))

            // then
            result shouldBe false
        }

        scenario("not all match property keys - empty campaign properties") {
            // given
            val campaign = Campaign.new(
                name = "testCampaign",
                properties = CampaignProperties(
                    value = emptyList()
                )
            )

            // when
            val result = campaign.allMatchPropertyKeys(listOf("key1", "key2"))

            // then
            result shouldBe false
        }

        scenario("not all match property keys - empty input keys") {
            // given
            val campaign = Campaign.new(
                name = "testCampaign",
                properties = CampaignProperties(
                    value = listOf(
                        CampaignProperty("key1", "value1"),
                        CampaignProperty("key2", "value2")
                    )
                )
            )

            // when
            val result = campaign.allMatchPropertyKeys(emptyList())

            // then
            result shouldBe false
        }
    }
})
