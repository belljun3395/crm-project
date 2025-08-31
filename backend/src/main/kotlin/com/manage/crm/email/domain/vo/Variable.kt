package com.manage.crm.email.domain.vo

const val TYPE_DELIMITER = "_"
const val DELIMITER = ":"

const val ALL_TYPES = "all"
const val USER_TYPE = "user"
const val CAMPAIGN_TYPE = "campaign"

abstract class Variable(
    val type: String,
    val key: String,
    val defaultValue: String? = null
) {
    companion object {
        fun extractType(value: String): String {
            return value.split(TYPE_DELIMITER)[0]
        }

        fun extractKey(value: String): String {
            return if (value.contains(DELIMITER)) {
                value.substringBefore(DELIMITER).substringAfter(TYPE_DELIMITER)
            } else {
                value.substringAfter(TYPE_DELIMITER)
            }
        }

        fun extractDefaultValue(value: String): String? {
            return if (value.contains(DELIMITER)) {
                value.substringAfter(DELIMITER)
            } else {
                null
            }
        }
    }

    fun hasDefault(): Boolean {
        return defaultValue != null
    }

    fun keyWithType(): String {
        return "$type$TYPE_DELIMITER$key"
    }

    fun displayValue(): String {
        return if (hasDefault()) {
            "$type$TYPE_DELIMITER$key$DELIMITER$defaultValue"
        } else {
            "$type$TYPE_DELIMITER$key"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Variable

        if (type != other.type) return false
        if (key != other.key) return false
        if (defaultValue != other.defaultValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }
}
