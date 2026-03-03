package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class UpdateJourneyLifecycleStatusUseCaseTest : BehaviorSpec({
    lateinit var journeyRepository: JourneyRepository
    lateinit var journeyStepRepository: JourneyStepRepository
    lateinit var useCase: UpdateJourneyLifecycleStatusUseCase

    beforeEach {
        journeyRepository = mockk()
        journeyStepRepository = mockk()
        useCase = UpdateJourneyLifecycleStatusUseCase(
            journeyRepository = journeyRepository,
            journeyStepRepository = journeyStepRepository,
            objectMapper = ObjectMapper()
        )
    }

    given("pause lifecycle") {
        `when`("active journey is paused") {
            then("mark status paused and increment version") {
                val journey = Journey(
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
                    version = 1
                )

                val step = JourneyStep.new(
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
                    retryCount = 0
                ).apply { id = 1L }

                coEvery { journeyRepository.findById(100L) } returns journey
                coEvery { journeyRepository.save(any()) } answers { firstArg() }
                coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(100L) } returns flowOf(step)

                val result = useCase.pause(100L)

                result.lifecycleStatus shouldBe JourneyLifecycleStatus.PAUSED.name
                result.active shouldBe false
                result.version shouldBe 2
            }
        }
    }

    given("resume lifecycle") {
        `when`("journey is archived") {
            then("throw invalid argument") {
                val journey = Journey(
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
                    version = 5
                )

                coEvery { journeyRepository.findById(11L) } returns journey

                shouldThrow<IllegalArgumentException> {
                    useCase.resume(11L)
                }
            }
        }
    }
})
