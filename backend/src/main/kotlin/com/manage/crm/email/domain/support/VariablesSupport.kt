package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.CAMPAIGN_TYPE
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.USER_TYPE
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variable
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.user.domain.vo.UserAttributes

fun List<String>.stringListToVariables(): Variables {
    return this.map {
        val type = Variable.extractType(it)
        val key = Variable.extractKey(it)
        val defaultValue = Variable.extractDefaultValue(it)
        return@map when (type) {
            USER_TYPE -> UserVariable(key, defaultValue)
            CAMPAIGN_TYPE -> CampaignVariable(key, defaultValue)
            else -> throw IllegalArgumentException("Type must be either $USER_TYPE or $CAMPAIGN_TYPE")
        }
    }
        .let { Variables(it) }
}

class VariablesSupport {
    companion object {
        fun associateUserAttribute(userAttribute: UserAttributes, userVariables: List<UserVariable>, objectMapper: ObjectMapper): Map<String, String> {
            val result = mutableMapOf<String, String>()
            for (userVariable in userVariables) {
                val value = when {
                    userAttribute.isExist(userVariable.key, objectMapper) -> {
                        userAttribute.getValue(userVariable.key, objectMapper)
                    }
                    else -> {
                        throw IllegalArgumentException("UserAttribute $userVariable not found")
                    }
                }
                result[userVariable.keyWithType()] = value
            }
            return result
        }

        fun associateCampaignEventProperty(properties: EventProperties, campaignVariables: Variables): Map<String, String> {
            val eventVariables = mutableMapOf<String, String>()
            val propertyKeys = properties.getKeys()
            for (campaignVariable in campaignVariables) {
                if (propertyKeys.contains(campaignVariable.key)) {
                    val value = properties.getValue(campaignVariable.key)
                    eventVariables[campaignVariable.keyWithType()] = value
                }
            }
            return eventVariables
        }
    }
}
