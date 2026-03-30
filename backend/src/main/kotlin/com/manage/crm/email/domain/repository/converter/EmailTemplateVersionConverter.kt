package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.EmailTemplateVersion
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class EmailTemplateVersionReadingConverter : Converter<Any, EmailTemplateVersion> {
    override fun convert(source: Any): EmailTemplateVersion {
        if (source is EmailTemplateVersion) {
            return source
        }

        return EmailTemplateVersion(source.toString().toFloat())
    }
}

@WritingConverter
class EmailTemplateVersionWritingConverter : Converter<EmailTemplateVersion, Float> {
    override fun convert(source: EmailTemplateVersion): Float {
        return source.value
    }
}
