package com.manage.crm.email.domain.vo

private fun String.isContainDefault(): Boolean {
    return this.contains(":")
}

private fun String.getKeyFromValue(): String {
    return this.substringBefore(":")
}

data class Variables(
    val value: List<String> = emptyList()
) {
    constructor(vararg value: String) : this(value.toList())

    fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    fun getVariables(withDefault: Boolean = true): List<String> {
        return if (withDefault) {
            value
        } else {
            value.map {
                if (it.isContainDefault()) {
                    it.getKeyFromValue()
                } else {
                    it
                }
            }
        }
    }

    fun getVariable(key: String, withDefault: Boolean = true): String? {
        return value.find { it.getKeyFromValue() == key }
            ?.let {
                if (withDefault) {
                    it
                } else {
                    it.getKeyFromValue()
                }
            }
    }
}
