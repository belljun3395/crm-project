package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty

class PropertiesFixtures private constructor() {
    private var value: List<EventProperty> = listOf(PropertyFixtures.anEventProperty().buildEvent())

    fun withValue(value: List<EventProperty>) = apply { this.value = value }

    fun buildEvent() = EventProperties(
        value = value
    )

    fun buildCampaign() = CampaignProperties(
        value = value.map { CampaignProperty(it.key, it.value) }
    )

    companion object {
        fun aProperties() = PropertiesFixtures()

        fun giveMeOne(): PropertiesFixtures {
            val properties = listOf(
                PropertyFixtures.giveMeOne().buildEvent(),
                PropertyFixtures.giveMeOne().buildEvent()
            )
            return aProperties()
                .withValue(properties)
        }

        fun giveMeOneCampaign(): PropertiesFixtures {
            val properties = listOf(
                PropertyFixtures.giveMeOneCampaign().buildCampaign(),
                PropertyFixtures.giveMeOneCampaign().buildCampaign()
            )
            return aProperties()
                .withValue(properties.map { EventProperty(it.key, it.value) })
        }

        fun giveMeOneEvent(): PropertiesFixtures {
            val properties = listOf(
                PropertyFixtures.giveMeOne().buildEvent(),
                PropertyFixtures.giveMeOne().buildEvent()
            )
            return aProperties()
                .withValue(properties)
        }

        fun giveMeOneCampaignProperties(): CampaignProperties {
            val properties = listOf(
                PropertyFixtures.giveMeOneCampaign().buildCampaign(),
                PropertyFixtures.giveMeOneCampaign().buildCampaign()
            )
            return CampaignProperties(properties)
        }

        fun giveMeOneEventProperties(): EventProperties {
            val properties = listOf(
                PropertyFixtures.giveMeOne().buildEvent(),
                PropertyFixtures.giveMeOne().buildEvent()
            )
            return EventProperties(properties)
        }
    }
}
