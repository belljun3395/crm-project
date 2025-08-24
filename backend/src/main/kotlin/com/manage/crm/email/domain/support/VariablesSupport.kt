package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.ATTRIBUTE_TYPE
import com.manage.crm.email.domain.vo.CUSTOM_ATTRIBUTE_TYPE
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.domain.vo.getType
import com.manage.crm.user.domain.vo.Json

class VariablesSupport {
    companion object {
        fun doAssociate(objectMapper: ObjectMapper, key: String, attributes: Json, variables: Variables): Pair<String, String> {
            return when {
                (key.getType() == ATTRIBUTE_TYPE || key.getType() == CUSTOM_ATTRIBUTE_TYPE) &&
                    attributes.isExist(key, objectMapper) -> {
                    key to attributes.getValue(key, objectMapper)
                }

                else -> {
                    key to (variables.findVariableDefault(key) ?: "")
                }
            }
        }

        fun variablesAllMatchedWithKey(variables: Variables, keys: Set<String>): Boolean {
            val variableKeys = variables.getVariables(false).toSet()
            return variableKeys.isNotEmpty() && variableKeys.all { keys.contains(it) }
        }
    }
}
