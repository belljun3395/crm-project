package com.manage.crm.user.domain.repository.converter

import com.manage.crm.user.domain.vo.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserAttributeReadingConverter : Converter<String, Json> {
    override fun convert(source: String): Json {
        return Json(source)
    }
}

@WritingConverter
class UserAttributeWritingConverter : Converter<Json, String> {
    override fun convert(source: Json): String {
        return source.value
    }
}

class JsonConverter
