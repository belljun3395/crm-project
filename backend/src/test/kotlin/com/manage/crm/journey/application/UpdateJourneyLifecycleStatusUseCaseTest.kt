package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyLifecycleAction
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.UpdateJourneyLifecycleStatusUseCaseIn
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class UpdateJourneyLifecycleStatusUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var useCase: UpdateJourneyLifecycleStatusUseCase

        beforeEach {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            useCase =
                UpdateJourneyLifecycleStatusUseCase(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    objectMapper = ObjectMapper(),
                )
        }

        given("UC-JOURNEY-006 pause lifecycle") {
            `when`("active journey is paused") {
                then("mark status paused and increment version") {
                    val currentJourney =
                        Journey(
                            id = 100L,
                            name = "welcome",
                            triggerType = JourneyTriggerType.EVENT.name,
                            triggerEventName = "SIGNUP",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = true,
                            lifecycleStatus = JourneyLifecycleStatus.ACTIVE.name,
                            version = 1,
                        )
                    val pausedJourney =
                        Journey(
                            id = 100L,
                            name = "welcome",
                            triggerType = JourneyTriggerType.EVENT.name,
                            triggerEventName = "SIGNUP",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = false,
                            lifecycleStatus = JourneyLifecycleStatus.PAUSED.name,
                            version = 2,
                        )

                    val step =
                        JourneyStep
                            .new(
                                journeyId = 100L,
                                stepOrder = 1,
                                stepType = JourneyStepType.ACTION.name,
                                channel = "EMAIL",
                                destination = "test@example.com",
                                subject = "hello",
                                body = "hi",
                                variablesJson = "{}",
                                delayMillis = null,
                                conditionExpression = null,
                                retryCount = 0,
                            ).apply { id = 1L }

                    coEvery { journeyRepository.findById(100L) } returnsMany listOf(currentJourney, pausedJourney)
                    coEvery {
                        journeyRepository.updateLifecycleStatusIfVersionMatches(
                            journeyId = 100L,
                            lifecycleStatus = JourneyLifecycleStatus.PAUSED.name,
                            active = false,
                            expectedVersion = 1,
                            newVersion = 2,
                        )
                    } returns 1
                    coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(100L) } returns flowOf(step)

                    val result =
                        useCase.execute(
                            UpdateJourneyLifecycleStatusUseCaseIn(
                                journeyId = 100L,
                                action = JourneyLifecycleAction.PAUSE,
                            ),
                        )

                    result.lifecycleStatus shouldBe JourneyLifecycleStatus.PAUSED.name
                    result.active shouldBe false
                    result.version shouldBe 2
                }
            }
        }

        given("pause lifecycle") {
            `when`("concurrent request updates journey first") {
                then("throw conflict error") {
                    val currentJourney =
                        Journey(
                            id = 100L,
                            name = "welcome",
                            triggerType = JourneyTriggerType.EVENT.name,
                            triggerEventName = "SIGNUP",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = true,
                            lifecycleStatus = JourneyLifecycleStatus.ACTIVE.name,
                            version = 1,
                        )

                    coEvery { journeyRepository.findById(100L) } returns currentJourney
                    coEvery {
                        journeyRepository.updateLifecycleStatusIfVersionMatches(
                            journeyId = 100L,
                            lifecycleStatus = JourneyLifecycleStatus.PAUSED.name,
                            active = false,
                            expectedVersion = 1,
                            newVersion = 2,
                        )
                    } returns 0

                    shouldThrow<IllegalStateException> {
                        useCase.execute(
                            UpdateJourneyLifecycleStatusUseCaseIn(
                                journeyId = 100L,
                                action = JourneyLifecycleAction.PAUSE,
                            ),
                        )
                    }
                }
            }
        }

        given("resume lifecycle") {
            `when`("journey is archived") {
                then("throw invalid argument") {
                    val journey =
                        Journey(
                            id = 11L,
                            name = "archived",
                            triggerType = JourneyTriggerType.EVENT.name,
                            triggerEventName = "SIGNUP",
                            triggerSegmentId = null,
                            triggerSegmentEvent = null,
                            triggerSegmentWatchFields = null,
                            triggerSegmentCountThreshold = null,
                            active = false,
                            lifecycleStatus = JourneyLifecycleStatus.ARCHIVED.name,
                            version = 5,
                        )

                    coEvery { journeyRepository.findById(11L) } returns journey

                    shouldThrow<IllegalArgumentException> {
                        useCase.execute(
                            UpdateJourneyLifecycleStatusUseCaseIn(
                                journeyId = 11L,
                                action = JourneyLifecycleAction.RESUME,
                            ),
                        )
                    }
                }
            }
        }
    })
