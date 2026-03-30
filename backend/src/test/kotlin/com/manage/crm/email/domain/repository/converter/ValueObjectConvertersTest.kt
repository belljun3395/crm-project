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
    }

    feature("UserEmailReadingConverter") {
        scenario("returns existing Email as-is") {
            val source = Email("user@example.com")

            val result = UserEmailReadingConverter().convert(source)

            result shouldBe source
        }
    }

    feature("EventIdReadingConverter") {
        scenario("returns existing EventId as-is") {
            val source = EventId("event-id-123")

            val result = EventIdReadingConverter().convert(source)

            result shouldBe source
        }
    }

    feature("EmailTemplateVersionReadingConverter") {
        scenario("returns existing EmailTemplateVersion as-is") {
            val source = EmailTemplateVersion(1.1f)

            val result = EmailTemplateVersionReadingConverter().convert(source)

            result shouldBe source
        }
    }
})
