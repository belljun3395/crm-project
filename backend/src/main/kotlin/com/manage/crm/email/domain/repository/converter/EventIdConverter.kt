package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.EventId
import io.r2dbc.spi.Row
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class EventIdReadingConverter : Converter<Row, EventId> {
    override fun convert(source: Row): EventId? {
        val value = source.get("event_id", String::class.java)
        return value?.let { EventId(it) }
    }
}

@WritingConverter
class EventIdWritingConverter : Converter<EventId, String> {
    override fun convert(source: EventId): String {
        return source.value
    }
}

class EventIdConverter
