package com.manage.crm.email.domain.vo

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class EventId(
    @JsonValue
    val value: String = UUID.randomUUID().toString()
)
