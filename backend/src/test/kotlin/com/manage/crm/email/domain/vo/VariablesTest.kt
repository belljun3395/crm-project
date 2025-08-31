package com.manage.crm.email.domain.vo

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariablesTest : FeatureSpec({

    feature(("Variables#isEmpty")) {
        scenario("check if variables is empty") {
            // given
            val variables = Variables()

            // then
            variables.isEmpty() shouldBe true
        }
    }

    feature("Variables#getVariables") {
        scenario("get all variables") {
            // given
            val userVariable = UserVariable("title", "hello")
            val campaignVariable = CampaignVariable("name")
            val variables = Variables(listOf(userVariable, campaignVariable))

            // when
            val result = variables.getVariables()

            // then
            result shouldBe listOf(userVariable, campaignVariable)
        }

        scenario("get user variables only") {
            // given
            val userVariable = UserVariable("title", "hello")
            val campaignVariable = CampaignVariable("name")
            val variables = Variables(listOf(userVariable, campaignVariable))

            // when
            val result = variables.getVariables(USER_TYPE)

            // then
            result shouldBe listOf(userVariable)
        }

        scenario("get campaign variables only") {
            // given
            val userVariable = UserVariable("title", "hello")
            val campaignVariable = CampaignVariable("name")
            val variables = Variables(listOf(userVariable, campaignVariable))

            // when
            val result = variables.getVariables(CAMPAIGN_TYPE)

            // then
            result shouldBe listOf(campaignVariable)
        }
    }

    feature("Variables#findVariable") {
        scenario("get user variable") {
            // given
            val userVariable = UserVariable("title", "hello")
            val variables = Variables(listOf(userVariable, UserVariable("name")))

            // when
            val result = variables.findVariable("title")

            // then
            result shouldBe userVariable
        }

        scenario("get campaign variable") {
            // given
            val campaignVariable = CampaignVariable("title", "hello")
            val variables = Variables(listOf(campaignVariable, CampaignVariable("name")))

            // when
            val result = variables.findVariable("title")

            // then
            result shouldBe campaignVariable
        }

        scenario("get variable that does not exist") {
            // given
            val variables = Variables(listOf(UserVariable("title", "hello")))

            // when
            val result = variables.findVariable("nonexistent")

            // then
            result shouldBe null
        }
    }

    feature("Variables#findVariableDefault") {
        scenario("get user variable default") {
            // given
            val variables = Variables(
                listOf(
                    UserVariable("title", "hello"),
                    UserVariable("name")
                )
            )

            // when
            val result = variables.findVariableDefault("title")

            // then
            result shouldBe "hello"
        }

        scenario("get campaign variable default") {
            // given
            val variables = Variables(
                listOf(
                    CampaignVariable("title", "hello"),
                    CampaignVariable("name")
                )
            )

            // when
            val result = variables.findVariableDefault("title")

            // then
            result shouldBe "hello"
        }

        scenario("get user variable default which does not have default value") {
            // given
            val variables = Variables(
                listOf(
                    UserVariable("title", "hello"),
                    UserVariable("name")
                )
            )

            // when
            val result = variables.findVariableDefault("name")

            // then
            result shouldBe null
        }

        scenario("get campaign variable default which does not have default value") {
            // given
            val variables = Variables(
                listOf(
                    CampaignVariable("title", "hello"),
                    CampaignVariable("name")
                )
            )

            // when
            val result = variables.findVariableDefault("name")

            // then
            result shouldBe null
        }
    }
})
