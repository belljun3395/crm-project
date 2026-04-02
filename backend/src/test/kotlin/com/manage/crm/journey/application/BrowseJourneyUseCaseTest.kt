package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class BrowseJourneyUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var useCase: BrowseJourneyUseCase

        beforeEach {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            useCase =
                BrowseJourneyUseCase(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    objectMapper = ObjectMapper(),
                )
        }

        given("UC-JOURNEY-003 browse journeys use case governance anchor") {
            `when`("test suite is discovered") {
                then("keeps UC traceability") {
                }
            }
        }

        given("UC-JOURNEY-003 browse journeys") {
            `when`("journeys exist") {
                then("returns journey list with steps") {
                    val journey =
                        Journey
                            .new(
                                name = "welcome-journey",
                                triggerType = "EVENT",
                                triggerEventName = "SIGNUP",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = true,
                            ).apply { id = 101L }

                    every { journeyRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(journey)
                    every {
                        journeyStepRepository.findAllByJourneyIdInOrderByJourneyIdAscStepOrderAsc(listOf(101L))
                    } returns flowOf()

                    val result =
                        useCase.execute(
                            com.manage.crm.journey.application.dto
                                .BrowseJourneyUseCaseIn(limit = 50),
                        )

                    result.journeys.size shouldBe 1
                    result.journeys[0].id shouldBe 101L
                    result.journeys[0].name shouldBe "welcome-journey"
                }
            }
        }
    })
