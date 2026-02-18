package com.manage.crm.email.domain.vo

enum class VariableSource(val value: String) {
    USER("user"),
    CAMPAIGN("campaign");

    companion object {
        fun fromValue(value: String): VariableSource =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException(
                    "Unknown variable source: $value. Must be one of: ${entries.map { it.value }}"
                )
    }
}
