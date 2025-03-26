package com.manage.crm.event.domain.repository.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

val objectMapper = ObjectMapper().apply {
    findAndRegisterModules()
    registerModules(JavaTimeModule())
}

@ReadingConverter
class PropertiesReadingConverter : Converter<String, Properties> {
    override fun convert(source: String): Properties {
        return Properties(
            objectMapper.readValue(source, List::class.java).stream()
                .map { objectMapper.convertValue(it, Map::class.java) }
                .map { Property(it["key"] as String, it["value"] as String) }
                .toList()
        )
    }
}

@WritingConverter
class PropertiesWritingConverter : Converter<Properties, String> {
    override fun convert(source: Properties): String {
        return objectMapper.writeValueAsString(source.value)
    }
}

class PropertiesConverter
