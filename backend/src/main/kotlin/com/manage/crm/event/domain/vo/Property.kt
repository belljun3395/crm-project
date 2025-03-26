package com.manage.crm.event.domain.vo

import com.fasterxml.jackson.annotation.JsonIgnore

data class Property(
    val key: String,
    val value: String
) {
    @JsonIgnore
    fun isNum(): Boolean {
        return value.matches(Regex("-?\\d+(\\.\\d+)?"))
    }
}
