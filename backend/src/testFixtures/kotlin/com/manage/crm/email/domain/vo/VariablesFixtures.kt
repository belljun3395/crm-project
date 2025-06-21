package com.manage.crm.email.domain.vo

class VariablesFixtures private constructor() {
    private var value: List<String> = emptyList()

    fun withValue(value: List<String>) = apply { this.value = value }

    fun build() = Variables(value)

    companion object {
        fun aVariables() = VariablesFixtures()

        fun giveMeOne(): VariablesFixtures {
            val value = emptyList<String>()
            return aVariables()
                .withValue(value)
        }
    }
}
