package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.support.stringListToVariables
import com.manage.crm.email.domain.vo.Variables
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class VariablesReadingConverter : Converter<String, Variables> {
    override fun convert(source: String): Variables {
        if (source.isEmpty()) {
            return Variables()
        }
        return source.split(",").map { it.trim() }.stringListToVariables()
    }
}

@WritingConverter
class VariablesWritingConverter : Converter<Variables, String> {
    override fun convert(source: Variables): String {
        return source.value.map { it.displayValue() }.toString().let { it.substring(1, it.length - 1) }
    }
}

class ArrayConverter
