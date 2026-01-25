package com.manage.crm.event.domain.vo

import com.fasterxml.jackson.annotation.JsonIgnore

data class EventProperty(
    val key: String,
    val value: String
) {
    @JsonIgnore
    fun isNum(): Boolean {
        return value.matches(Regex("-?\\d+(\\.\\d+)?"))
    }
}

data class CampaignProperty(
    val key: String,
    val value: String
) {
    @JsonIgnore
    fun isNum(): Boolean {
        return value.matches(Regex("-?\\d+(\\.\\d+)?"))
    }
}
