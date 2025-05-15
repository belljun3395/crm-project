package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Variables
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class EmailTemplateTest : FeatureSpec({
    feature("EmailTemplate#new") {
        scenario("create new email template") {
            // given
            val templateName = "templateName"
            val subject = "subject"
            val body = "body"
            val variables = Variables()

            // when
            val emailTemplate = EmailTemplate.new(templateName, subject, body, Variables(variables.value))

            // then
            emailTemplate.templateName shouldBe templateName
            emailTemplate.subject shouldBe subject
            emailTemplate.body shouldBe body
            emailTemplate.variables shouldBe variables
            emailTemplate.version!!.value shouldBe 1.0f
        }
    }

    feature("EmailTemplate#isNewTemplate") {
        scenario("check if email template is new") {
            // given
            val templateName = "templateName"
            val subject = "subject"
            val body = "body"
            val variables = Variables()
            val emailTemplate = EmailTemplate.new(templateName, subject, body, Variables(variables.value))

            // then
            emailTemplate.isNewTemplate() shouldBe true
        }
    }

    feature("EmailTemplate#modify") {
        val variables = Variables()

        scenario("modify email template subject") {
            // given
            val subject = "newSubject"
            val emailTemplate = EmailTemplate.new("templateName", "subject", "body", Variables(variables.value))
            emailTemplate.id = 1

            // when
            val modifiedTemplate = emailTemplate.modify()
                .modifySubject(subject)
                .done()

            // then
            modifiedTemplate.subject shouldBe subject
        }

        scenario("modify email template body with variables") {
            // given
            val body = "newBody"
            val emailTemplate = EmailTemplate.new("templateName", "subject", "body", Variables(variables.value))
            emailTemplate.id = 1

            // when
            val modifiedTemplate = emailTemplate.modify()
                .modifyBody(body, variables)
                .done()

            // then
            modifiedTemplate.body shouldBe body
        }

        scenario("modify email template version") {
            // given
            val version = 1.1f
            val emailTemplate = EmailTemplate.new("templateName", "subject", "body", Variables(variables.value))
            emailTemplate.id = 1

            // when
            val modifiedTemplate = emailTemplate.modify()
                .updateVersion(version)
                .done()

            // then
            modifiedTemplate.version!!.value shouldBe version
        }

        scenario("modify email template version under current version") {
            // given
            val version = 0.9f
            val emailTemplate = EmailTemplate.new("templateName", "subject", "body", Variables(variables.value))
            emailTemplate.id = 1

            // when
            val exception = shouldThrow<IllegalArgumentException> {
                emailTemplate.modify()
                    .updateVersion(version)
                    .done()
            }

            // then
            exception.message shouldBe "Invalid version: $version"
        }
    }
})
