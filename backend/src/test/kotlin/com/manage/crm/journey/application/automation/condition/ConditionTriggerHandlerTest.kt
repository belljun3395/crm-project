package com.manage.crm.journey.application.automation.condition

import com.manage.crm.journey.application.dto.JourneyTriggerEvent
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class ConditionTriggerHandlerTest :
    BehaviorSpec({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var handler: ConditionTriggerHandler

        beforeTest {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            handler =
                ConditionTriggerHandler(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    conditionExpressionResolver = ConditionExpressionResolver(),
                    conditionEvaluator = ConditionEvaluator(),
                )
        }

        given("condition-triggered journey processing") {
            `when`("condition matches event properties") {
                then("callback is invoked once with expected trigger key") {
                    val journey =
                        Journey
                            .new(
                                name = "condition-journey",
                                triggerType = "CONDITION",
                                triggerEventName = null,
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = true,
                            ).apply { id = 77L }
                    val branchStep =
                        JourneyStep.new(
                            journeyId = 77L,
                            stepOrder = 1,
                            stepType = "BRANCH",
                            channel = null,
                            destination = null,
                            subject = null,
                            body = null,
                            variablesJson = null,
                            delayMillis = null,
                            conditionExpression = "event.plan == \"pro\"",
                            retryCount = 0,
                        )
                    val event =
                        JourneyTriggerEvent(
                            id = 100L,
                            name = "purchase",
                            userId = 10L,
                            properties = mapOf("plan" to "pro"),
                            createdAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0),
                        )

                    coEvery { journeyRepository.findAllByTriggerTypeAndActiveTrue("CONDITION") } returns flowOf(journey)
                    coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(77L) } returns flowOf(branchStep)

                    val triggerKeys = mutableListOf<String>()
                    handler.processConditionTriggeredJourneys(event) { _, _, triggerKey ->
                        triggerKeys.add(triggerKey)
                    }

                    triggerKeys shouldBe listOf("77:CONDITION:100:10")
                }
            }

            `when`("expression is invalid") {
                then("it skips callback and keeps processing safe") {
                    val journey =
                        Journey
                            .new(
                                name = "broken-condition-journey",
                                triggerType = "CONDITION",
                                triggerEventName = "event.plan > \"pro\"",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = true,
                            ).apply { id = 88L }
                    val event =
                        JourneyTriggerEvent(
                            id = 101L,
                            name = "purchase",
                            userId = 10L,
                            properties = mapOf("plan" to "pro"),
                            createdAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0),
                        )

                    coEvery { journeyRepository.findAllByTriggerTypeAndActiveTrue("CONDITION") } returns flowOf(journey)
                    coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(any()) } returns emptyFlow()

                    var called = false
                    handler.processConditionTriggeredJourneys(event) { _, _, _ ->
                        called = true
                    }

                    called shouldBe false
                }
            }
        }
    })
