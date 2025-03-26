package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.ATTRIBUTE_TYPE
import com.manage.crm.email.domain.vo.CUSTOM_ATTRIBUTE_TYPE
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.domain.vo.getAttributeKey
import com.manage.crm.email.domain.vo.getCustomAttributeKey
import com.manage.crm.email.domain.vo.getKeyType
import com.manage.crm.user.domain.vo.Json

class VariablesSupport {
    companion object {
        fun doAssociate(objectMapper: ObjectMapper, key: String, attributes: Json, variables: Variables): Pair<String, String> {
            return when {
                key.getKeyType() == ATTRIBUTE_TYPE && attributes.isExist(key.getAttributeKey(), objectMapper) -> {
                    key to attributes.getValue(key.getAttributeKey(), objectMapper)
                }

                key.getKeyType() == CUSTOM_ATTRIBUTE_TYPE && attributes.isExist(key.getCustomAttributeKey(), objectMapper) -> {
                    key to attributes.getValue(key.getCustomAttributeKey(), objectMapper)
                }

                else -> {
                    key to (variables.findVariableDefault(key) ?: "")
                }
            }
        }
    }
}
