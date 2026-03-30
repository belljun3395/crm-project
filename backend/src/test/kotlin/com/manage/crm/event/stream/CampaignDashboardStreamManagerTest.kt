package com.manage.crm.event.stream

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CampaignDashboardStreamManagerTest : BehaviorSpec({
    given("CampaignDashboardStreamManager companion") {
        `when`("getStreamKey") {
            then("builds key with campaign id") {
                CampaignDashboardStreamManager.getStreamKey(1L) shouldBe
                    "campaign:dashboard:stream:1"
                CampaignDashboardStreamManager.getStreamKey(999L) shouldBe
                    "campaign:dashboard:stream:999"
            }
        }
    }
})
