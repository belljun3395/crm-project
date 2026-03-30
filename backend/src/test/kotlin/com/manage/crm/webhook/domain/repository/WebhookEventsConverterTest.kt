package com.manage.crm.webhook.domain.repository

import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.r2dbc.postgresql.codec.Json

class WebhookEventsConverterTest : FeatureSpec({
    feature("WebhookEvents converters") {
        scenario("write WebhookEvents as PostgreSQL Json") {
            val source = WebhookEvents(listOf(WebhookEventType.USER_CREATED, WebhookEventType.EMAIL_SENT))

            val result = WebhookEventsWritingConverter().convert(source)

            result.asString() shouldBe """["USER_CREATED","EMAIL_SENT"]"""
        }

        scenario("read WebhookEvents from PostgreSQL Json") {
            val source = Json.of("""["USER_CREATED","EMAIL_SENT"]""")

            val result = WebhookEventsReadingConverter().convert(source)

            result shouldBe WebhookEvents(listOf(WebhookEventType.USER_CREATED, WebhookEventType.EMAIL_SENT))
        }
    }
})
