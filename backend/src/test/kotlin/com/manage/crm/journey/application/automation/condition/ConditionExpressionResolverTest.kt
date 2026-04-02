package com.manage.crm.journey.application.automation.condition

import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ConditionExpressionResolverTest :
    BehaviorSpec({
        val resolver = ConditionExpressionResolver()

        given("condition expression resolution") {
            `when`("journey has triggerEventName") {
                then("it returns triggerEventName first") {
                    val journey =
                        Journey.new(
                            name = "condition-journey",
                            triggerType = "CONDITION",
                            triggerEventName = "event.plan == \"pro\"",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = true,
                        )

                    val resolved = resolver.resolve(journey, emptyList())

                    resolved shouldBe "event.plan == \"pro\""
                }
            }

            `when`("journey triggerEventName is blank") {
                then("it resolves first branch condition expression") {
                    val journey =
                        Journey.new(
                            name = "condition-journey",
                            triggerType = "CONDITION",
                            triggerEventName = " ",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = true,
                        )

                    val branchStep =
                        JourneyStep.new(
                            journeyId = 1L,
                            stepOrder = 2,
                            stepType = JourneyStepType.BRANCH.name,
                            channel = null,
                            destination = null,
                            subject = null,
                            body = null,
                            variablesJson = null,
                            delayMillis = null,
                            conditionExpression = "event.amount != \"0\"",
                            retryCount = 0,
                        )

                    val actionStep =
                        JourneyStep.new(
                            journeyId = 1L,
                            stepOrder = 1,
                            stepType = JourneyStepType.ACTION.name,
                            channel = "EMAIL",
                            destination = "sample@example.com",
                            subject = null,
                            body = "hello",
                            variablesJson = "{}",
                            delayMillis = null,
                            conditionExpression = null,
                            retryCount = 0,
                        )

                    val resolved = resolver.resolve(journey, listOf(actionStep, branchStep))

                    resolved shouldBe "event.amount != \"0\""
                }
            }
        }
    })
