package com.manage.crm.email.domain.vo

data class Variables(
    val value: List<Variable> = emptyList()
) : Iterable<Variable> {

    override fun iterator(): Iterator<Variable> {
        return value.iterator()
    }

    fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    fun getDisplayVariables(type: String = ALL_TYPES): List<String> {
        return when (type) {
            USER_TYPE -> filterByType(USER_TYPE).map { it.displayValue() }
            CAMPAIGN_TYPE -> filterByType(CAMPAIGN_TYPE).map { it.displayValue() }
            ALL_TYPES -> value.map { it.displayValue() }
            else -> throw IllegalArgumentException("Type must be either $USER_TYPE or $CAMPAIGN_TYPE")
        }
    }

    fun getVariables(type: String = ALL_TYPES): List<Variable> {
        return when (type) {
            USER_TYPE -> filterByType(USER_TYPE)
            CAMPAIGN_TYPE -> filterByType(CAMPAIGN_TYPE)
            ALL_TYPES -> value
            else -> throw IllegalArgumentException("Type must be either $USER_TYPE or $CAMPAIGN_TYPE")
        }
    }

    fun findVariable(key: String): Variable? {
        return value.find { it.key == key }
    }

    fun findVariableDefault(key: String): String? {
        return value.find { it.key == key }?.defaultValue
    }

    fun filterByType(type: String): List<Variable> {
        return when (type) {
            USER_TYPE -> value.filter { it is UserVariable }
            CAMPAIGN_TYPE -> value.filter { it is CampaignVariable }
            else -> throw IllegalArgumentException("Invalid type: $type")
        }
    }
}
