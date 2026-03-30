package com.manage.crm.user.domain.repository.converter

import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class JsonConverterTest : FeatureSpec({
    val readingConverter = UserAttributeReadingConverter()
    val writingConverter = UserAttributeWritingConverter()

    feature("UserAttributeReadingConverter#convert") {
        scenario("converts any database value to UserAttributes via toString") {
            val source = object {
                override fun toString(): String = """{"email":"example@example.com"}"""
            }

            val result = readingConverter.convert(source)

            result shouldBe UserAttributes(source.toString())
        }
    }

    feature("UserAttributeWritingConverter#convert") {
        scenario("extracts the raw json string value") {
            val source = UserAttributes("""{"email":"example@example.com"}""")

            val result = writingConverter.convert(source)

            result shouldBe source.value
        }
    }
})
