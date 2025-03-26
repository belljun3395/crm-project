package com.manage.crm.event.domain

enum class Operation(
    val value: String,
    val paramsCnt: Int
) {
    EQUALS("=", 1),
    NOT_EQUALS("!=", 1),
    GREATER_THAN(">", 1),
    GREATER_THAN_OR_EQUALS(">=", 1),
    LESS_THAN("<", 1),
    LESS_THAN_OR_EQUALS("<=", 1),
    LIKE("LIKE", 1),
    BETWEEN("BETWEEN", 2);

    companion object {
        fun fromValue(value: String): Operation {
            return entries.find { it.value == value.uppercase() } ?: throw IllegalArgumentException("Invalid operation: $value")
        }
    }
}
