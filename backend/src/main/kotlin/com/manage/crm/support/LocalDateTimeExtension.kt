package com.manage.crm.support

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

fun LocalDateTimeExtension.parseExpiredTime(date: String): LocalDateTime = LocalDateTime.parse(date, LocalDateTimeExtension.timeFormatter)

fun String.parseISOExpiredTime(): LocalDateTime = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)

class LocalDateTimeExtension {
    companion object {
        val timeFormatter: DateTimeFormatter =
            DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .toFormatter()
    }
}
