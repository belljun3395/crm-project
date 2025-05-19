package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.Email
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserEmailReadingConverter : Converter<String, Email> {
    override fun convert(source: String): Email? {
        return if (source.isEmpty()) {
            null
        } else {
            Email(source)
        }
    }
}

@WritingConverter
class UserEmailWritingConverter : Converter<Email, String> {
    override fun convert(source: Email): String {
        return source.value
    }
}

class EmailConverter
