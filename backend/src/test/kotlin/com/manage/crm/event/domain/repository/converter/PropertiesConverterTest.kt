package com.manage.crm.event.domain.repository.converter

import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.r2dbc.postgresql.codec.Json

class PropertiesConverterTest : FeatureSpec({
    feature("EventProperties converters") {
        scenario("return existing EventProperties as-is") {
            val source = EventProperties(listOf(EventProperty("product_id", "12345")))

            val result = EventPropertiesReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("write EventProperties as PostgreSQL Json") {
            val source = EventProperties(listOf(EventProperty("product_id", "12345")))

            val result = EventPropertiesWritingConverter().convert(source)

            result.asString() shouldBe """[{"key":"product_id","value":"12345"}]"""
        }

        scenario("read EventProperties from PostgreSQL Json") {
            val source = Json.of("""[{"key":"product_id","value":"12345"}]""")

            val result = EventPropertiesReadingConverter().convert(source)

            result shouldBe EventProperties(listOf(EventProperty("product_id", "12345")))
        }
    }

    feature("CampaignProperties converters") {
        scenario("return existing CampaignProperties as-is") {
            val source = CampaignProperties(listOf(CampaignProperty("audience", "premium")))

            val result = CampaignPropertiesReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("write CampaignProperties as PostgreSQL Json") {
            val source = CampaignProperties(listOf(CampaignProperty("audience", "premium")))

            val result = CampaignPropertiesWritingConverter().convert(source)

            result.asString() shouldBe """[{"key":"audience","value":"premium"}]"""
        }

        scenario("read CampaignProperties from PostgreSQL Json") {
            val source = Json.of("""[{"key":"audience","value":"premium"}]""")

            val result = CampaignPropertiesReadingConverter().convert(source)

            result shouldBe CampaignProperties(listOf(CampaignProperty("audience", "premium")))
        }
    }
})
