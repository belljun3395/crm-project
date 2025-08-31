package com.manage.crm.user.domain.vo

import com.fasterxml.jackson.databind.ObjectMapper

data class UserAttributes(
    val value: String
) {
    fun isExist(key: String, objectMapper: ObjectMapper): Boolean {
        objectMapper.readValue(value, Map::class.java)[key]?.let {
            return true
        } ?: return false
    }

    fun isExist(keys: List<String>, objectMapper: ObjectMapper): Boolean {
        var map = objectMapper.readValue(value, Map::class.java)
        val lastKey = keys.last()
        for (i in 0 until keys.size - 1) {
            map[keys[i]]?.let {
                map = objectMapper.readValue(it.toString(), Map::class.java)
            } ?: throw IllegalArgumentException("Key not found. key: ${keys[i]}")
        }
        return map.containsKey(lastKey)
    }

    fun getValue(key: RequiredUserAttributeKey, objectMapper: ObjectMapper): String {
        return objectMapper.readValue(value, Map::class.java)[key.value] as String
    }

    fun getValue(key: String, objectMapper: ObjectMapper): String {
        return objectMapper.readValue(value, Map::class.java)[key].toString()
    }

    fun getValue(keys: List<String>, objectMapper: ObjectMapper): String {
        var map = objectMapper.readValue(value, Map::class.java)
        val lastKey = keys.last()
        for (i in 0 until keys.size - 1) {
            map[keys[i]]?.let {
                map = objectMapper.readValue(it.toString(), Map::class.java)
            } ?: throw IllegalArgumentException("Key not found. key: ${keys[i]}")
        }
        return map[lastKey].toString()
    }
}
