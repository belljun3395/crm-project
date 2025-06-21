package com.manage.crm.email.domain.vo

import kotlin.random.Random

class EmailTemplateVersionFixtures private constructor() {
    private var value: Float = 1.0f

    fun withValue(value: Float) = apply { this.value = value }

    fun build() = EmailTemplateVersion(value)

    companion object {
        fun anEmailTemplateVersion() = EmailTemplateVersionFixtures()

        fun giveMeOne(): EmailTemplateVersionFixtures {
            val value = Random.nextFloat() + 1.0f

            return anEmailTemplateVersion()
                .withValue(value)
        }
    }
}
