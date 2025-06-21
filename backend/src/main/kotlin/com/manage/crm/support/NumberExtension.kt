package com.manage.crm.support

fun Any?.asLong(): Long {
    return when (this) {
        is Int -> this.toLong()
        is Long -> this
        is String -> this.toLongOrNull() ?: throw NumberFormatException("Invalid number format: $this")
        else -> throw IllegalArgumentException("Unsupported type for conversion to Long: $this")
    }
}

class NumberExtension