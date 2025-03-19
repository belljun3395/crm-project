package com.manage.crm.email.domain.vo

/**
 * Variables의 value는 다음과 같은 형태로 저장된다.
 * - "title:hello", "name"
 *
 * "title:hello"는 key와 default value로 구성되어 있다.
 *
 * containDefault에서는 key와 default value를 구분하기 위한 delimiter를 사용하여 default value가 있는지 확인한다.
 */
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
