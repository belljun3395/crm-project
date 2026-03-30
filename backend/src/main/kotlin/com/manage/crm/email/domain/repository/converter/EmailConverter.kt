package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.Email
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserEmailReadingConverter : Converter<Any, Email> {
    override fun convert(source: Any): Email? {
        if (source is Email) {
            return source
        }

        val text = source.toString()
        return if (text.isEmpty()) {
            null
        } else {
            Email(text)
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
