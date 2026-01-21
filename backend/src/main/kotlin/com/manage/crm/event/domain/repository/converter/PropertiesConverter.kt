package com.manage.crm.event.domain.repository.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

val objectMapper = ObjectMapper().apply {
    findAndRegisterModules()
    registerModules(JavaTimeModule())
}

@ReadingConverter
class EventPropertiesReadingConverter : Converter<String, EventProperties> {
    override fun convert(source: String): EventProperties {
        return EventProperties(
            objectMapper.readValue(source, List::class.java).stream()
                .map { objectMapper.convertValue(it, Map::class.java) }
                .map { EventProperty(it["key"] as String, it["value"] as String) }
                .toList()
        )
    }
}

@WritingConverter
class EventPropertiesWritingConverter : Converter<EventProperties, String> {
    override fun convert(source: EventProperties): String {
        return objectMapper.writeValueAsString(source.value)
    }
}

@ReadingConverter
class CampaignPropertiesReadingConverter : Converter<String, CampaignProperties> {
    override fun convert(source: String): CampaignProperties {
        return CampaignProperties(
            objectMapper.readValue(source, List::class.java).stream()
                .map { objectMapper.convertValue(it, Map::class.java) }
                .map { CampaignProperty(it["key"] as String, it["value"] as String) }
                .toList()
        )
    }
}

@WritingConverter
class CampaignPropertiesWritingConverter : Converter<CampaignProperties, String> {
    override fun convert(source: CampaignProperties): String {
        return objectMapper.writeValueAsString(source.value)
    }
}

class PropertiesConverter

class EventPropertiesConverter

class CampaignPropertiesConverter
