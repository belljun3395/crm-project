package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property

class PropertiesFixtures private constructor() {
    private lateinit var value: List<Property>

    fun withValue(value: List<Property>) = apply { this.value = value }

    fun build() = Properties(
        value = value
    )

    companion object {
        fun aProperties() = PropertiesFixtures()

        fun giveMeOne(): PropertiesFixtures {
            val properties = listOf(
                PropertyFixtures.giveMeOne().build(),
                PropertyFixtures.giveMeOne().build()
            )
            return aProperties()
                .withValue(properties)
        }
    }
}
