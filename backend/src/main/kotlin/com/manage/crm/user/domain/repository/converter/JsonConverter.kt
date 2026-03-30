package com.manage.crm.user.domain.repository.converter

import com.manage.crm.user.domain.vo.UserAttributes
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserAttributeReadingConverter : Converter<Any, UserAttributes> {
    override fun convert(source: Any): UserAttributes {
        return when (source) {
            is UserAttributes -> source
            is Json -> UserAttributes(source.asString())
            else -> UserAttributes(source.toString())
        }
    }
}

@WritingConverter
class UserAttributeWritingConverter : Converter<UserAttributes, Json> {
    override fun convert(source: UserAttributes): Json {
        return Json.of(source.value)
    }
}

class JsonConverter
