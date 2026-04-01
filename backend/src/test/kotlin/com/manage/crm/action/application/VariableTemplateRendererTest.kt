package com.manage.crm.action.application

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class VariableTemplateRendererTest :
    BehaviorSpec({
        given("VariableTemplateRenderer") {
            `when`("template includes known and unknown variables") {
                then("render known values and replace unknown values with empty string") {
                    val rendered =
                        VariableTemplateRenderer.render(
                            template = "Hello {{ name }}, your code is {{code}} and {{missing}}",
                            variables = mapOf("name" to "crm", "code" to "A-1"),
                        )

                    rendered shouldBe "Hello crm, your code is A-1 and "
                }
            }

            `when`("template has no variable token") {
                then("return template as-is") {
                    val rendered =
                        VariableTemplateRenderer.render(
                            template = "no variable",
                            variables = mapOf("name" to "ignored"),
                        )

                    rendered shouldBe "no variable"
                }
            }
        }
    })
