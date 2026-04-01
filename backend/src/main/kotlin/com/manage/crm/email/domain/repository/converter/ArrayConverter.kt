package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.support.stringListToVariables
import com.manage.crm.email.domain.vo.Variables
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class VariablesReadingConverter : Converter<Any, Variables> {
    override fun convert(source: Any): Variables {
        if (source is Variables) {
            return source
        }

        val text = source.toString()
        if (text.isEmpty()) {
            return Variables()
        }
        return text.split(",").map { it.trim() }.stringListToVariables()
    }
}

@WritingConverter
class VariablesWritingConverter : Converter<Variables, String> {
    override fun convert(source: Variables): String =
        source.value
            .map { it.displayValue() }
            .toString()
            .let { it.substring(1, it.length - 1) }
}
