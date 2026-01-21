package com.manage.crm.event.domain.vo

import java.util.stream.Collectors

data class EventProperties(
    val value: List<EventProperty>
) {
    fun getValue(key: String): String {
        return value.stream()
            .filter { it.key == key }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Event property does not contain key: $key") }
            .value
    }

    fun getKeys(): List<String> {
        return value.stream()
            .map { it.key }
            .collect(Collectors.toList())
    }
}

data class CampaignProperties(
    val value: List<CampaignProperty>
) {
    fun getValue(key: String): String {
        return value.stream()
            .filter { it.key == key }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Campaign property does not contain key: $key") }
            .value
    }

    fun getKeys(): List<String> {
        return value.stream()
            .map { it.key }
            .collect(Collectors.toList())
    }
}
