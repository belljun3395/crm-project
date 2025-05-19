package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.EventId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class EventIdReadingConverter : Converter<String, EventId> {
    override fun convert(source: String): EventId? {
        return if (source.isEmpty()) {
            null
        } else {
            EventId(source)
        }
    }
}

@WritingConverter
class EventIdWritingConverter : Converter<EventId, String> {
    override fun convert(source: EventId): String {
        return source.value
    }
}

class EventIdConverter
