package com.manage.crm.webhook.domain.repository

import com.manage.crm.event.domain.repository.converter.objectMapper
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class WebhookEventsReadingConverter : Converter<String, WebhookEvents> {
    override fun convert(source: String): WebhookEvents {
        val values = objectMapper.readValue(source, List::class.java)
            .map { it.toString() }
        val eventTypes = values.map { WebhookEventType.fromValue(it) }
        return WebhookEvents(eventTypes)
    }
}

@WritingConverter
class WebhookEventsWritingConverter : Converter<WebhookEvents, String> {
    override fun convert(source: WebhookEvents): String {
        return objectMapper.writeValueAsString(source.toValues())
    }
}

class WebhookEventsConverter
