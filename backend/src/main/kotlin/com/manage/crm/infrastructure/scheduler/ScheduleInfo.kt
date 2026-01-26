package com.manage.crm.infrastructure.scheduler

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput

/**
 * Base class for schedule payload information.
 * Uses Jackson polymorphic type handling for JSON serialization/deserialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotificationEmailSendTimeOutEventInput::class, name = "notification-email-timeout")
)
abstract class ScheduleInfo

/**
 * Represents a schedule name identifier.
 */
data class ScheduleName(val value: String)
