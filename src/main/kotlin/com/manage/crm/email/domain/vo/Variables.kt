package com.manage.crm.email.domain.vo

private fun String.containDefault(delimiter: String): Boolean {
    return this.contains(delimiter)
}

private fun String.extractKey(delimiter: String): String {
    return this.substringBefore(delimiter)
}

private const val DELIMITER = ":"

data class Variables(
    val value: List<String> = emptyList()
) {
    constructor(vararg value: String) : this(value.toList())

    fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    fun getVariables(withDefault: Boolean = true, delimiter: String = DELIMITER): List<String> {
        return if (withDefault) {
            value
        } else {
            value.map {
                if (it.containDefault(delimiter)) {
                    it.extractKey(delimiter)
                } else {
                    it
                }
            }
        }
    }

    fun findVariable(key: String, withDefault: Boolean = true, delimiter: String = DELIMITER): String? {
        return value.find { it == key || it.startsWith("$key$delimiter") }
            ?.let {
                if (withDefault) {
                    it
                } else {
                    it.extractKey(delimiter)
                }
            }
    }
}
