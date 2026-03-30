package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.EventId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class EventIdReadingConverter : Converter<Any, EventId> {
    override fun convert(source: Any): EventId? {
        if (source is EventId) {
            return source
        }

        val text = source.toString()
        return if (text.isEmpty()) {
            null
        } else {
            EventId(text)
        }
    }
}

@WritingConverter
class EventIdWritingConverter : Converter<EventId, String> {
    override fun convert(source: EventId): String {
        return source.value
    }
}
