package com.manage.crm.journey.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class JourneyStepTest :
    BehaviorSpec({
        given("JourneyStep#new creates action step") {
            `when`("creating with valid action payload") {
                val step =
                    JourneyStep.new(
                        journeyId = 10L,
                        stepOrder = 1,
                        stepType = "ACTION",
                        channel = "EMAIL",
                        destination = "sample@example.com",
                        subject = "hello",
                        body = "body",
                        variablesJson = "{}",
                        delayMillis = null,
                        conditionExpression = null,
                        retryCount = 0,
                    )

                then("it keeps provided fields") {
                    step.id.shouldBeNull()
                    step.journeyId shouldBe 10L
                    step.stepOrder shouldBe 1
                    step.stepType shouldBe "ACTION"
                    step.channel shouldBe "EMAIL"
                    step.destination shouldBe "sample@example.com"
                    step.retryCount shouldBe 0
                }
            }
        }

        given("JourneyStep#new receives invalid order") {
            `when`("stepOrder is zero") {
                val step =
                    JourneyStep.new(
                        journeyId = 10L,
                        stepOrder = 0,
                        stepType = "DELAY",
                        channel = null,
                        destination = null,
                        subject = null,
                        body = null,
                        variablesJson = null,
                        delayMillis = 100L,
                        conditionExpression = null,
                        retryCount = 0,
                    )

                then("current domain constructor preserves the incoming invalid order") {
                    step.stepOrder shouldBe 0
                    step.createdAt.shouldBeNull()
                }
            }
        }
    })
