package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.PutJourneyStepIn
import com.manage.crm.journey.application.dto.PutJourneyUseCaseIn
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class PutJourneyUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository
        lateinit var useCase: PutJourneyUseCase

        beforeEach {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            journeyExecutionHistoryRepository = mockk()
            useCase =
                PutJourneyUseCase(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    journeyExecutionHistoryRepository = journeyExecutionHistoryRepository,
                    objectMapper = ObjectMapper(),
                )
        }

        given("UC-JOURNEY-002 update journey use case governance anchor") {
            `when`("test suite is discovered") {
                then("keeps UC traceability") {
                }
            }
        }

        given("UC-JOURNEY-002 update journey") {
            `when`("existing DRAFT journey is updated with active=true") {
                then("lifecycle status becomes ACTIVE and version increments") {
                    val existing =
                        Journey
                            .new(
                                name = "old-name",
                                triggerType = "EVENT",
                                triggerEventName = "SIGNUP",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = false,
                                lifecycleStatus = JourneyLifecycleStatus.DRAFT.name,
                                version = 1,
                            ).apply { id = 101L }

                    coEvery { journeyRepository.findById(101L) } returns existing
                    coEvery { journeyRepository.save(any()) } answers { firstArg<Journey>().apply { id = 101L } }
                    every { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(101L) } returns flowOf()
                    coEvery { journeyStepRepository.save(any()) } answers {
                        firstArg<JourneyStep>().apply { id = 201L }
                    }

                    val result =
                        useCase.execute(
                            PutJourneyUseCaseIn(
                                journeyId = 101L,
                                name = "renamed-journey",
                                triggerType = JourneyTriggerType.EVENT,
                                triggerEventName = "SIGNUP",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = emptyList(),
                                triggerSegmentCountThreshold = null,
                                active = true,
                                steps =
                                    listOf(
                                        PutJourneyStepIn(
                                            stepOrder = 1,
                                            stepType = JourneyStepType.ACTION,
                                            channel = "EMAIL",
                                            destination = "sample@example.com",
                                            subject = "welcome",
                                            body = "hello",
                                            variables = emptyMap(),
                                            delayMillis = null,
                                            conditionExpression = null,
                                            retryCount = 0,
                                        ),
                                    ),
                            ),
                        )

                    result.journey.id shouldBe 101L
                    result.journey.name shouldBe "renamed-journey"
                    result.journey.active shouldBe true
                    result.journey.lifecycleStatus shouldBe JourneyLifecycleStatus.ACTIVE.name
                    result.journey.version shouldBe 2
                }
            }
        }
    })
