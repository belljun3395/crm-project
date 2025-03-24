package com.manage.crm.email.domain.repository.converter

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
        val variables = source.split(",").map { it.trim() }
        return Variables(variables)
    }
}

@WritingConverter
class VariablesWritingConverter : Converter<Variables, String> {
    override fun convert(source: Variables): String {
        return source.value.joinToString(",")
    }
}

class ArrayConverter
