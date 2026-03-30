package com.manage.crm.infrastructure.jooq

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

internal fun Map<String, Any>.requireLocalDateTime(column: String): LocalDateTime {
    return get(column).toLocalDateTimeOrNull()
        ?: error("Expected $column to be a date-time but was ${get(column)?.javaClass}")
}

internal fun Map<String, Any>.optionalLocalDateTime(column: String): LocalDateTime? {
    return get(column).toLocalDateTimeOrNull()
}

private fun Any?.toLocalDateTimeOrNull(): LocalDateTime? {
    return when (this) {
        null -> null
        is LocalDateTime -> this
        is OffsetDateTime -> toLocalDateTime()
        is ZonedDateTime -> toLocalDateTime()
        is Instant -> OffsetDateTime.ofInstant(this, OffsetDateTime.now().offset).toLocalDateTime()
        is Timestamp -> toLocalDateTime()
        else -> null
    }
}
