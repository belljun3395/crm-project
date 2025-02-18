package com.manage.crm.email.application.service

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class HtmlServiceTest : FeatureSpec({
    val htmlService = HtmlService()

    feature("HtmlService#extractVariables") {
        scenario("extracts variables from html") {
            val input = """
                <html>
                    <head>
                        <title>${"$"}{title}</title>
                    </head>
                    <body>
                        <h1>Hello, ${"$"}{name}</h1>
                        <p>How are you?</p>
                    </body>
                </html>
            """.trimIndent()

            val variables = htmlService.extractVariables(input)

            variables shouldBe listOf("title", "name")
        }

        scenario("invalid variable format - not closed tag") {
            val input = """
                <html>
                    <head>
                        <title>${"$"}{title}</title>
                    </head>
                    <body>
                        <h1>Hello, ${"$"}{name</h1>
                        <p>How are you?</p>
                    </body>
                </html>
            """.trimIndent()

            val variables = htmlService.extractVariables(input)

            variables shouldBe listOf("title")
        }
    }
})
