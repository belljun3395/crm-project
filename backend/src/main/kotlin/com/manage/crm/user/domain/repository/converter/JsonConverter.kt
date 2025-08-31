package com.manage.crm.user.domain.repository.converter

import com.manage.crm.user.domain.vo.UserAttributes
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserAttributeReadingConverter : Converter<String, UserAttributes> {
    override fun convert(source: String): UserAttributes {
        return UserAttributes(source)
    }
}

@WritingConverter
class UserAttributeWritingConverter : Converter<UserAttributes, String> {
    override fun convert(source: UserAttributes): String {
        return source.value
    }
}

class JsonConverter
