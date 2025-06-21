package com.manage.crm.email.domain.vo

import java.util.UUID

class EventIdFixtures private constructor() {
    private var value: String = "00000000-0000-0000-0000-000000000000"

    fun withValue(value: String) = apply { this.value = value }

    fun build() = EventId(value)

    companion object {
        fun anEventId() = EventIdFixtures()

        fun giveMeOne(): EventIdFixtures {
            val value = UUID.randomUUID().toString()
            return anEventId()
                .withValue(value)
        }
    }
}
