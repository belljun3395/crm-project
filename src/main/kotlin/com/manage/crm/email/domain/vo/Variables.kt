package com.manage.crm.email.domain.vo

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
                if (it.contains(":")) {
                    it.substringBefore(":")
                } else {
                    it
                }
            }
        }
    }

    fun getVariable(key: String, withDefault: Boolean = true): String? {
        return value.find { it.substringBefore(":") == key }
            ?.let {
                if (withDefault) {
                    it
                } else {
                    it.substringBefore(":")
                }
            }
    }
}
