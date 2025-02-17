package com.manage.crm.user.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import org.springframework.stereotype.Component

@Component
class JsonService(
    private val objectMapper: ObjectMapper
) {

    fun execute(attribute: String, vararg keys: RequiredUserAttributeKey): Json {
        attribute.let {
            try {
                val json = objectMapper.readValue(it, Map::class.java)
                for (key in keys) {
                    if (!json.containsKey(key.value)) {
                        throw IllegalArgumentException("Attribute does not contain key: ${key.value}")
                    }
                }
                return Json(it)
            } catch (e: Exception) {
                if (e.message?.contains("Attribute does not contain key") == true) {
                    throw e
                }
                throw IllegalArgumentException("Attribute is not JSON format: $it")
            }
        }
    }
}
