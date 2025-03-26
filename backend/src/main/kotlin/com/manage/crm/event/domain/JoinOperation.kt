package com.manage.crm.event.domain

enum class JoinOperation(val value: String) {
    AND("AND"),
    OR("OR"),
    END("END");

    companion object {
        fun fromValue(value: String): JoinOperation {
            return entries.find { it.value == value.uppercase() } ?: throw IllegalArgumentException("Invalid operation: $value")
        }
    }
}
