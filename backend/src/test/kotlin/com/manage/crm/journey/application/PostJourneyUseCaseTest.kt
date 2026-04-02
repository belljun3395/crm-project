package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.PostJourneyStepIn
import com.manage.crm.journey.application.dto.PostJourneyUseCaseIn
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class PostJourneyUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var useCase: PostJourneyUseCase

        beforeEach {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            useCase =
                PostJourneyUseCase(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    objectMapper = ObjectMapper(),
                )
        }

        given("UC-JOURNEY-001 create journey request with inactive flag") {
            `when`("executed") {
                then("store lifecycle status as draft") {
                    coEvery { journeyRepository.save(any()) } answers {
                        firstArg<Journey>().apply { id = 101L }
                    }
                    coEvery { journeyStepRepository.save(any()) } answers {
                        firstArg<JourneyStep>().apply { id = 201L }
                    }

                    val result =
                        useCase.execute(
                            PostJourneyUseCaseIn(
                                name = "welcome-journey",
                                triggerType = JourneyTriggerType.EVENT,
                                triggerEventName = "SIGNUP",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = emptyList(),
                                triggerSegmentCountThreshold = null,
                                active = false,
                                steps =
                                    listOf(
                                        PostJourneyStepIn(
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

                    result.journey.active shouldBe false
                    result.journey.lifecycleStatus shouldBe JourneyLifecycleStatus.DRAFT.name
                    result.journey.version shouldBe 1
                }
            }
        }
    })
