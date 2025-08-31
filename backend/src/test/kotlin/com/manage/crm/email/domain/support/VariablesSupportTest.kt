package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariablesSupportTest : FeatureSpec({

    val objectMapper = ObjectMapper()

    feature("VariablesSupport#associateUserAttribute") {
        scenario("associate with attribute key that exists in attributes") {
            // given
            val userVariable = UserVariable("name")
            val attributes = UserAttributes("""{"name": "John Doe"}""")

            // when
            val result = VariablesSupport.associateUserAttribute(attributes, listOf(userVariable), objectMapper)
            val resultPair = userVariable.keyWithType() to result[userVariable.keyWithType()]!!

            // then
            resultPair shouldBe ("user_name" to "John Doe")
        }

        scenario("associateUserAttribute with attribute key that does not exist in attributes") {
            // given
            val userVariable = UserVariable("name")
            val attributes = UserAttributes("""{"email": "john@example.com"}""")

            // when & then
            shouldThrow<IllegalArgumentException> {
                VariablesSupport.associateUserAttribute(attributes, listOf(userVariable), objectMapper)
            }
        }
    }

    feature("VariablesSupport#associateCampaignEventProperty") {
        scenario("associate with property key that exists in campaign properties") {
            // given
            val campaignVariable = CampaignVariable("eventCount")
            val properties = Properties(listOf(Property("eventCount", "10")))

            // when
            val result = VariablesSupport.associateCampaignEventProperty(properties, Variables(listOf(campaignVariable)))

            // then
            result[campaignVariable.keyWithType()] shouldBe "10"
        }

        scenario("associate with property key that does not exist in campaign properties") {
            // given
            val campaignVariable = CampaignVariable("eventCount")
            val properties = Properties(listOf(Property("totalCount", "5")))

            // when
            val result = VariablesSupport.associateCampaignEventProperty(properties, Variables(listOf(campaignVariable)))

            // then
            result.isEmpty() shouldBe true
        }

        scenario("associate with multiple campaign properties") {
            // given
            val campaignVariables = listOf(
                CampaignVariable("eventCount"),
                CampaignVariable("totalRevenue")
            )
            val properties = Properties(
                listOf(
                    Property("eventCount", "10"),
                    Property("totalRevenue", "1000.50")
                )
            )

            // when
            val result = VariablesSupport.associateCampaignEventProperty(properties, Variables(campaignVariables))

            // then
            result["campaign_eventCount"] shouldBe "10"
            result["campaign_totalRevenue"] shouldBe "1000.50"
        }
    }
})
