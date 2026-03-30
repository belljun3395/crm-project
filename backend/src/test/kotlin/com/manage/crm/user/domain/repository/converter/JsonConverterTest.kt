package com.manage.crm.user.domain.repository.converter

import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.r2dbc.postgresql.codec.Json

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

        scenario("reads PostgreSQL Json values") {
            val source = Json.of("""{"email":"json@example.com"}""")

            val result = readingConverter.convert(source)

            result shouldBe UserAttributes(source.asString())
        }
    }

    feature("UserAttributeWritingConverter#convert") {
        scenario("returns PostgreSQL Json values") {
            val source = UserAttributes("""{"email":"example@example.com"}""")

            val result = writingConverter.convert(source)

            result.asString() shouldBe source.value
        }
    }
})
