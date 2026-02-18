package com.manage.crm.email.domain.support

import com.manage.crm.email.domain.vo.VariableSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariableParserTest : FeatureSpec({

    feature("VariableParser#parse - new format (source.key)") {
        scenario("parse user variable in new format") {
            val (source, key, default) = VariableParser.parse("user.email")
            source shouldBe VariableSource.USER
            key shouldBe "email"
            default shouldBe null
        }

        scenario("parse campaign variable in new format") {
            val (source, key, default) = VariableParser.parse("campaign.eventCount")
            source shouldBe VariableSource.CAMPAIGN
            key shouldBe "eventCount"
            default shouldBe null
        }

        scenario("parse user variable with default value in new format") {
            val (source, key, default) = VariableParser.parse("user.email:test@example.com")
            source shouldBe VariableSource.USER
            key shouldBe "email"
            default shouldBe "test@example.com"
        }

        scenario("parse campaign variable with default value in new format") {
            val (source, key, default) = VariableParser.parse("campaign.eventCount:0")
            source shouldBe VariableSource.CAMPAIGN
            key shouldBe "eventCount"
            default shouldBe "0"
        }
    }

    feature("VariableParser#parse - legacy format (source_key)") {
        scenario("parse user variable in legacy format") {
            val (source, key, default) = VariableParser.parse("user_email")
            source shouldBe VariableSource.USER
            key shouldBe "email"
            default shouldBe null
        }

        scenario("parse campaign variable in legacy format") {
            val (source, key, default) = VariableParser.parse("campaign_eventCount")
            source shouldBe VariableSource.CAMPAIGN
            key shouldBe "eventCount"
            default shouldBe null
        }

        scenario("parse user variable with default value in legacy format") {
            val (source, key, default) = VariableParser.parse("user_email:test@example.com")
            source shouldBe VariableSource.USER
            key shouldBe "email"
            default shouldBe "test@example.com"
        }

        scenario("parse campaign variable with default value in legacy format") {
            val (source, key, default) = VariableParser.parse("campaign_eventCount:0")
            source shouldBe VariableSource.CAMPAIGN
            key shouldBe "eventCount"
            default shouldBe "0"
        }
    }

    feature("VariableParser#parse - round-trip consistency") {
        scenario("new and legacy format produce the same result") {
            val newFormat = VariableParser.parse("user.email")
            val legacyFormat = VariableParser.parse("user_email")
            newFormat shouldBe legacyFormat
        }

        scenario("new and legacy format with default produce the same result") {
            val newFormat = VariableParser.parse("campaign.eventCount:0")
            val legacyFormat = VariableParser.parse("campaign_eventCount:0")
            newFormat shouldBe legacyFormat
        }
    }

    feature("VariableParser#parse - invalid formats") {
        scenario("unknown source throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                VariableParser.parse("product.price")
            }
        }

        scenario("invalid format (curly brace prefix) throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                VariableParser.parse("{user_email}")
            }
        }
    }
})
