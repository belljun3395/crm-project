package com.manage.crm.email.domain.vo

/**
 * Variables의 value는 다음과 같은 형태로 저장된다.
 * - "type_title:hello", "name"
 *
 * "type_title:hello"는 key, default value로 구성되어 있다.
 *
 * Variables에서 key는 attribute, custom attribute로 구분하기 위해 type을 포함한다.
 *
 * containDefault에서는 key, default value를 구분하기 위한 delimiter를 사용하여 default value가 있는지 확인한다.
 */
private fun String.containDefault(delimiter: String = DELIMITER): Boolean {
    return this.contains(delimiter)
}

private fun String.extractKey(delimiter: String = DELIMITER): String {
    return this.substringBefore(delimiter)
}

fun String.getType(): String {
    return this.split(TYPE_DELIMITER)[0] + TYPE_DELIMITER
}

private const val DELIMITER = ":"
private const val TYPE_DELIMITER = "_"
const val ATTRIBUTE_TYPE = "attribute$TYPE_DELIMITER"
const val CUSTOM_ATTRIBUTE_TYPE = "custom$TYPE_DELIMITER"

data class Variables(
    val value: List<String> = emptyList()
) {
    constructor(vararg value: String) : this(value.toList())

    init {
        checkValueContainType()
    }

    private fun checkValueContainType() {
        value.forEach {
            if (!(it.contains(ATTRIBUTE_TYPE) || it.contains(CUSTOM_ATTRIBUTE_TYPE))) {
                throw IllegalArgumentException("Value need to contain $TYPE_DELIMITER for distinguish variable type.")
            }

            if (it.contains(CUSTOM_ATTRIBUTE_TYPE)) {
                if (it.split(TYPE_DELIMITER).size > 2) {
                    throw IllegalArgumentException("Custom type format is invalid.")
                }
            }
        }
    }

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

    fun findVariable(
        key: String,
        withDefault: Boolean = true,
        delimiter: String = DELIMITER
    ): String? {
        return value.find { it == key || it.startsWith("$key$delimiter") }
            ?.let {
                if (withDefault) {
                    it
                } else {
                    it.extractKey(delimiter)
                }
            }
    }

    fun findVariableDefault(key: String, delimiter: String = DELIMITER): String? {
        return value.find { it.startsWith("$key$delimiter") }?.substringAfter(delimiter)
    }
}
