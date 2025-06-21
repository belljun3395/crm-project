package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.Property
import java.util.UUID

class PropertyFixtures private constructor() {
    private lateinit var key: String
    private lateinit var value: String

    fun withKey(key: String) = apply { this.key = key }
    fun withValue(value: String) = apply { this.value = value }

    fun build() = Property(
        key = key,
        value = value
    )

    companion object {
        fun aProperty() = PropertyFixtures()

        fun giveMeOne(): PropertyFixtures {
            val randomKey = "key" + UUID.randomUUID().toString().substring(0, 5)
            val randomValue = "value" + UUID.randomUUID().toString().substring(0, 5)
            return aProperty()
                .withKey(randomKey)
                .withValue(randomValue)
        }
    }
}
