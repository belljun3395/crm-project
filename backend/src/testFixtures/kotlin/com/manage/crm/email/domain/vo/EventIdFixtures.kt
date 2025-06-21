package com.manage.crm.email.domain.vo

import java.util.UUID

class EventIdFixtures private constructor() {
    private lateinit var value: String

    fun withValue(value: String) = apply { this.value = value }

    fun build() = EventId(value)

    companion object {
        fun anEventId() = EventIdFixtures()
            .withValue(UUID.randomUUID().toString())

        fun giveMeOne(): EventIdFixtures {
            val value = UUID.randomUUID().toString()
            return anEventId()
                .withValue(value)
        }
    }
}
