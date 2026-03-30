package com.manage.crm.webhook.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

private val objectMapper = ObjectMapper().apply {
    findAndRegisterModules()
    registerModules(JavaTimeModule())
}

@ReadingConverter
class WebhookEventsReadingConverter : Converter<Any, WebhookEvents> {
    override fun convert(source: Any): WebhookEvents {
        if (source is WebhookEvents) {
            return source
        }

        val values = objectMapper.readValue(
            when (source) {
                is Json -> source.asString()
                else -> source.toString()
            },
            List::class.java
        )
            .map { it.toString() }
        val eventTypes = values.map { WebhookEventType.fromValue(it) }
        return WebhookEvents(eventTypes)
    }
}

@WritingConverter
class WebhookEventsWritingConverter : Converter<WebhookEvents, Json> {
    override fun convert(source: WebhookEvents): Json {
        return Json.of(objectMapper.writeValueAsString(source.toValues()))
    }
}
