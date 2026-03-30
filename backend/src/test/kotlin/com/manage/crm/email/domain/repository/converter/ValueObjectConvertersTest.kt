package com.manage.crm.email.domain.repository.converter

import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.Email
import com.manage.crm.email.domain.vo.EmailTemplateVersion
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variables
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class ValueObjectConvertersTest : FeatureSpec({
    feature("VariablesReadingConverter") {
        scenario("returns existing Variables as-is") {
            val source = Variables(
                listOf(
                    UserVariable("email"),
                    CampaignVariable("targetAudience")
                )
            )

            val result = VariablesReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("reads persisted variable declarations") {
            val result = VariablesReadingConverter().convert("user.email,campaign.targetAudience")

            result shouldBe Variables(
                listOf(
                    UserVariable("email"),
                    CampaignVariable("targetAudience")
                )
            )
        }

        scenario("returns empty Variables for empty string") {
            val result = VariablesReadingConverter().convert("")

            result shouldBe Variables()
        }
    }

    feature("VariablesWritingConverter") {
        scenario("writes Variables as comma-separated display values") {
            val source = Variables(listOf(UserVariable("email"), CampaignVariable("targetAudience")))

            val result = VariablesWritingConverter().convert(source)

            result shouldBe "user.email,campaign.targetAudience"
        }
    }

    feature("UserEmailReadingConverter") {
        scenario("returns existing Email as-is") {
            val source = Email("user@example.com")

            val result = UserEmailReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("reads email string from database") {
            val result = UserEmailReadingConverter().convert("user@example.com")

            result shouldBe Email("user@example.com")
        }

        scenario("returns null for empty string") {
            val result = UserEmailReadingConverter().convert("")

            result shouldBe null
        }
    }

    feature("UserEmailWritingConverter") {
        scenario("writes Email as plain string") {
            val result = UserEmailWritingConverter().convert(Email("user@example.com"))

            result shouldBe "user@example.com"
        }
    }

    feature("EventIdReadingConverter") {
        scenario("returns existing EventId as-is") {
            val source = EventId("event-id-123")

            val result = EventIdReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("reads EventId string from database") {
            val result = EventIdReadingConverter().convert("event-id-456")

            result shouldBe EventId("event-id-456")
        }

        scenario("returns null for empty string") {
            val result = EventIdReadingConverter().convert("")

            result shouldBe null
        }
    }

    feature("EventIdWritingConverter") {
        scenario("writes EventId as plain string") {
            val result = EventIdWritingConverter().convert(EventId("event-id-123"))

            result shouldBe "event-id-123"
        }
    }

    feature("EmailTemplateVersionReadingConverter") {
        scenario("returns existing EmailTemplateVersion as-is") {
            val source = EmailTemplateVersion(1.1f)

            val result = EmailTemplateVersionReadingConverter().convert(source)

            result shouldBe source
        }

        scenario("reads version float from database string") {
            val result = EmailTemplateVersionReadingConverter().convert("2.5")

            result shouldBe EmailTemplateVersion(2.5f)
        }
    }

    feature("EmailTemplateVersionWritingConverter") {
        scenario("writes EmailTemplateVersion as float") {
            val result = EmailTemplateVersionWritingConverter().convert(EmailTemplateVersion(1.1f))

            result shouldBe 1.1f
        }
    }
})
