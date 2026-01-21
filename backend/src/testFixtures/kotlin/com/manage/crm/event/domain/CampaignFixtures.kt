package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.CampaignProperties
import java.time.LocalDateTime
import kotlin.random.Random

class CampaignFixtures private constructor() {
    private var id: Long = -1L
    private var name: String = "default-campaign-name"
    private var properties: CampaignProperties = PropertiesFixtures.giveMeOne().buildCampaign()
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withProperties(properties: CampaignProperties) = apply { this.properties = properties }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun build(): Campaign = Campaign(
        id = id,
        name = name,
        properties = properties,
        createdAt = createdAt
    )

    companion object {
        fun aCampaign() = CampaignFixtures()

        fun giveMeOne(): CampaignFixtures {
            val id = Random.nextLong(1, 101)
            val name = "campaign_name" + Random.nextLong(1, 101)
            val properties = PropertiesFixtures.giveMeOne().buildCampaign()
            return aCampaign()
                .withId(id)
                .withName(name)
                .withProperties(properties)
        }
    }
}
