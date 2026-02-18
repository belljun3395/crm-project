package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariableResolverRegistryTest : FeatureSpec({

    val objectMapper = ObjectMapper()
    val registry = VariableResolverRegistry(
        listOf(UserVariableResolver(), CampaignVariableResolver())
    )

    feature("VariableResolverRegistry#resolveAll") {
        scenario("resolve user variables produces legacy keys for Thymeleaf substitution") {
            // given
            val variables = Variables(listOf(UserVariable("name"), UserVariable("email")))
            val context = VariableResolverContext(
                userAttributes = UserAttributes("""{"name": "John", "email": "john@example.com"}"""),
                objectMapper = objectMapper
            )

            // when
            val result = registry.resolveAll(variables, context)

            // then - legacy format keys used for ${user_name} / ${user_email} in HTML templates
            result["user_name"] shouldBe "John"
            result["user_email"] shouldBe "john@example.com"
        }

        scenario("resolve campaign variables produces legacy keys for Thymeleaf substitution") {
            // given
            val variables = Variables(listOf(CampaignVariable("eventCount")))
            val context = VariableResolverContext(
                eventProperties = EventProperties(listOf(EventProperty("eventCount", "42")))
            )

            // when
            val result = registry.resolveAll(variables, context)

            // then - legacy format key used for ${campaign_eventCount} in HTML templates
            result["campaign_eventCount"] shouldBe "42"
        }

        scenario("resolve mixed user and campaign variables") {
            // given
            val variables = Variables(
                listOf(
                    UserVariable("name"),
                    CampaignVariable("eventCount")
                )
            )
            val context = VariableResolverContext(
                userAttributes = UserAttributes("""{"name": "Jane"}"""),
                eventProperties = EventProperties(listOf(EventProperty("eventCount", "5"))),
                objectMapper = objectMapper
            )

            // when
            val result = registry.resolveAll(variables, context)

            // then
            result["user_name"] shouldBe "Jane"
            result["campaign_eventCount"] shouldBe "5"
        }

        scenario("missing user attribute throws IllegalArgumentException") {
            // given
            val variables = Variables(listOf(UserVariable("phone")))
            val context = VariableResolverContext(
                userAttributes = UserAttributes("""{"name": "John"}"""),
                objectMapper = objectMapper
            )

            // when & then
            shouldThrow<IllegalArgumentException> {
                registry.resolveAll(variables, context)
            }
        }

        scenario("missing campaign property returns empty (relaxed policy)") {
            // given
            val variables = Variables(listOf(CampaignVariable("missingKey")))
            val context = VariableResolverContext(
                eventProperties = EventProperties(listOf(EventProperty("eventCount", "10")))
            )

            // when
            val result = registry.resolveAll(variables, context)

            // then
            result.isEmpty() shouldBe true
        }

        scenario("empty variables returns empty map") {
            // given
            val variables = Variables()
            val context = VariableResolverContext(objectMapper = objectMapper)

            // when
            val result = registry.resolveAll(variables, context)

            // then
            result.isEmpty() shouldBe true
        }
    }
})
