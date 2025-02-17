package com.manage.crm.user.domain.vo

import com.fasterxml.jackson.databind.ObjectMapper

data class Json(
    val value: String
) {
    fun getValue(key: RequiredUserAttributeKey, objectMapper: ObjectMapper): String {
        return objectMapper.readValue(value, Map::class.java)[key.value] as String
    }
}
