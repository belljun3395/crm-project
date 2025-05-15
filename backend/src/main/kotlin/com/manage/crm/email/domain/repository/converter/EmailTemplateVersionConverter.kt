package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.EmailTemplateVersion
import io.r2dbc.spi.Row
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class EmailTemplateVersionReadingConverter : Converter<Row, EmailTemplateVersion> {
    override fun convert(source: Row): EmailTemplateVersion? {
        val value = source.get("version", Float::class.java)
        return value?.let { EmailTemplateVersion(it) }
    }
}

@WritingConverter
class EmailTemplateVersionWritingConverter : Converter<EmailTemplateVersion, Float> {
    override fun convert(source: EmailTemplateVersion): Float {
        return source.value
    }
}

class EmailTemplateVersionConverter
