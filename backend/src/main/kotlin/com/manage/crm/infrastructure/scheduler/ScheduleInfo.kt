package com.manage.crm.infrastructure.scheduler

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = NotificationEmailSendTimeOutEventInput::class, name = "notification-email-timeout")
)
abstract class ScheduleInfo

data class ScheduleName(
    val value: String
)
