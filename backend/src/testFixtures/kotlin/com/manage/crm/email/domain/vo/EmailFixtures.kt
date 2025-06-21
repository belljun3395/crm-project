package com.manage.crm.email.domain.vo

import java.util.UUID

class EmailFixtures private constructor() {
    private lateinit var value: String

    fun withValue(value: String) = apply { this.value = value }

    fun build() = Email(value)

    companion object {
        fun anEmail() = EmailFixtures()

        fun giveMeOne(): EmailFixtures {
            val randomPart = UUID.randomUUID().toString().substring(0, 8)
            val email = "$randomPart@example.com"
            return anEmail()
                .withValue(email)
        }
    }
}
