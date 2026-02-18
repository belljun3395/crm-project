package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.service.CampaignVariableResolver
import com.manage.crm.email.application.service.UserVariableResolver
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariableResolverTest : FeatureSpec({

    val objectMapper = ObjectMapper()
    val userVariableResolver = UserVariableResolver()
    val campaignVariableResolver = CampaignVariableResolver()

    feature("UserVariableResolver#resolve") {
        scenario("resolve with attribute key that exists in attributes") {
            // given
            val userVariable = UserVariable("name")
            val attributes = UserAttributes("""{"name": "John Doe"}""")
            val context = VariableResolverContext(userAttributes = attributes, objectMapper = objectMapper)

            // when
            val result = userVariableResolver.resolve(userVariable, context)

            // then - legacy format key for Thymeleaf template substitution
            result["user_name"] shouldBe "John Doe"
        }

        scenario("resolve with attribute key that does not exist in attributes") {
            // given
            val userVariable = UserVariable("name")
            val attributes = UserAttributes("""{"email": "john@example.com"}""")
            val context = VariableResolverContext(userAttributes = attributes, objectMapper = objectMapper)

            // when & then
            shouldThrow<IllegalArgumentException> {
                userVariableResolver.resolve(userVariable, context)
            }
        }
    }

    feature("CampaignVariableResolver#resolve") {
        scenario("resolve with property key that exists in campaign properties") {
            // given
            val campaignVariable = CampaignVariable("eventCount")
            val properties = EventProperties(listOf(EventProperty("eventCount", "10")))
            val context = VariableResolverContext(eventProperties = properties)

            // when
            val result = campaignVariableResolver.resolve(campaignVariable, context)

            // then - legacy format key for Thymeleaf template substitution
            result["campaign_eventCount"] shouldBe "10"
        }

        scenario("resolve with property key that does not exist in campaign properties") {
            // given
            val campaignVariable = CampaignVariable("eventCount")
            val properties = EventProperties(listOf(EventProperty("totalCount", "5")))
            val context = VariableResolverContext(eventProperties = properties)

            // when
            val result = campaignVariableResolver.resolve(campaignVariable, context)

            // then
            result.isEmpty() shouldBe true
        }

        scenario("resolve with multiple campaign properties") {
            // given
            val campaignVariables = listOf(
                CampaignVariable("eventCount"),
                CampaignVariable("totalRevenue")
            )
            val properties = EventProperties(
                listOf(
                    EventProperty("eventCount", "10"),
                    EventProperty("totalRevenue", "1000.50")
                )
            )
            val context = VariableResolverContext(eventProperties = properties)

            // when & then
            val resultA = campaignVariableResolver.resolve(campaignVariables[0], context)
            resultA["campaign_eventCount"] shouldBe "10"

            val resultB = campaignVariableResolver.resolve(campaignVariables[1], context)
            resultB["campaign_totalRevenue"] shouldBe "1000.50"
        }

        scenario("resolve returns empty map when no event properties in context") {
            // given
            val campaignVariable = CampaignVariable("eventCount")
            val context = VariableResolverContext(eventProperties = null)

            // when
            val result = campaignVariableResolver.resolve(campaignVariable, context)

            // then
            result.isEmpty() shouldBe true
        }
    }
})
