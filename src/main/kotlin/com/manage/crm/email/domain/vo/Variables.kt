package com.manage.crm.email.domain.vo

const val DELIMITER = ":"

private fun String.containDefault(delimiter: String = DELIMITER): Boolean {
    return this.contains(DELIMITER)
}

private fun String.extractKey(delimiter: String = DELIMITER): String {
    return this.substringBefore(delimiter)
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
                if (it.containDefault()) {
                    it.extractKey()
                } else {
                    it
                }
            }
        }
    }

    fun findVariable(key: String, withDefault: Boolean = true): String? {
        return value.find { it.extractKey() == key }
            ?.let {
                if (withDefault) {
                    it
                } else {
                    it.extractKey()
                }
            }
    }
}
