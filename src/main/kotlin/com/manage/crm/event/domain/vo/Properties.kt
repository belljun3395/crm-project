package com.manage.crm.event.domain.vo

import java.util.stream.Collectors

data class Properties(
    val value: List<Property>
) {
    fun getValue(key: String): String {
        return value.stream()
            .filter { it.key == key }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Property does not contain key: $key") }
            .value
    }

    fun getKeys(): List<String> {
        return value.stream()
            .map { it.key }
            .collect(Collectors.toList())
    }
}
