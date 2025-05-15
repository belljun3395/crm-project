package com.manage.crm.user.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import com.manage.crm.user.exception.JsonException
import org.springframework.stereotype.Component

@Component
class JsonService(
    private val objectMapper: ObjectMapper
) {
    /**
     * `attribute`를 JSON으로 변환하고, `keys`에 해당하는 키가 포함되어 있는지 확인합니다.
     */
    fun execute(attribute: String, vararg keys: RequiredUserAttributeKey): Json {
        attribute.let {
            try {
                val json = objectMapper.readValue(it, Map::class.java)
                for (key in keys) {
                    if (!json.containsKey(key.value)) {
                        throw JsonException.notContainKey(key.value)
                    }
                }
                return Json(it)
            } catch (e: Exception) {
                if (e.message?.contains("Attribute does not contain key") == true) {
                    throw e
                }
                throw JsonException.notJsonFormat(attribute)
            }
        }
    }
}
