package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.Email
import io.r2dbc.spi.Row
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UserEmailReadingConverter : Converter<Row, Email> {
    override fun convert(source: Row): Email? {
        val value = source.get("user_email", String::class.java)
        return value?.let { Email(it) }
    }
}

@WritingConverter
class UserEmailWritingConverter : Converter<Email, String> {
    override fun convert(source: Email): String {
        return source.value
    }
}

class EmailConverter
