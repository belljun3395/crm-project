package com.manage.crm.email.domain.vo

class VariablesFixtures private constructor() {
    private var value: List<Variable> = emptyList()

    fun withValue(value: List<Variable>) = apply { this.value = value }

    fun build() = Variables(value)

    companion object {
        fun aVariables() = VariablesFixtures()

        fun giveMeOne(): VariablesFixtures {
            val value = emptyList<Variable>()
            return aVariables()
                .withValue(value)
        }

        fun withUserVariables(): VariablesFixtures {
            val value = listOf(
                UserVariable("name"),
                UserVariable("email"),
                UserVariable("phone", "010-0000-0000")
            )
            return aVariables()
                .withValue(value)
        }

        fun withCampaignVariables(): VariablesFixtures {
            val value = listOf(
                CampaignVariable("title"),
                CampaignVariable("description", "Default campaign description")
            )
            return aVariables()
                .withValue(value)
        }

        fun withMixedVariables(): VariablesFixtures {
            val value = listOf(
                UserVariable("name"),
                UserVariable("email"),
                CampaignVariable("title"),
                CampaignVariable("description", "Default description")
            )
            return aVariables()
                .withValue(value)
        }
    }
}
