package com.manage.crm.journey.application.automation.segment

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneySegmentCountState
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneySegmentCountStateRepository
import com.manage.crm.journey.domain.repository.JourneySegmentUserStateRepository
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.user.application.port.query.UserReadPort
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class SegmentTriggerHandlerTest :
    BehaviorSpec({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeySegmentUserStateRepository: JourneySegmentUserStateRepository
        lateinit var journeySegmentCountStateRepository: JourneySegmentCountStateRepository
        lateinit var segmentReadPort: SegmentReadPort
        lateinit var userReadPort: UserReadPort
        lateinit var handler: SegmentTriggerHandler

        beforeTest {
            journeyRepository = mockk()
            journeySegmentUserStateRepository = mockk(relaxed = true)
            journeySegmentCountStateRepository = mockk(relaxed = true)
            segmentReadPort = mockk()
            userReadPort = mockk()

            handler =
                SegmentTriggerHandler(
                    journeyRepository = journeyRepository,
                    journeySegmentUserStateRepository = journeySegmentUserStateRepository,
                    journeySegmentCountStateRepository = journeySegmentCountStateRepository,
                    segmentReadPort = segmentReadPort,
                    userReadPort = userReadPort,
                    objectMapper = ObjectMapper(),
                )
        }

        given("segment COUNT_REACHED trigger") {
            `when`("threshold is crossed") {
                then("execute callback with expected trigger key") {
                    val journey =
                        Journey
                            .new(
                                name = "segment-count-journey",
                                triggerType = "SEGMENT",
                                triggerEventName = null,
                                triggerSegmentId = 900L,
                                triggerSegmentEvent = "COUNT_REACHED",
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = 2L,
                                active = true,
                            ).apply { id = 55L }

                    coEvery { journeyRepository.findAllByTriggerTypeAndActiveTrue("SEGMENT") } returns flowOf(journey)
                    coEvery { segmentReadPort.findTargetUserIds(eq(900L)) } returns listOf(1L, 2L)
                    coEvery { journeySegmentCountStateRepository.findByJourneyId(55L) } returns
                        JourneySegmentCountState.new(journeyId = 55L, lastCount = 1L, transitionVersion = 0L)
                    coEvery { journeySegmentCountStateRepository.save(any()) } answers { firstArg() }

                    val triggerKeys = mutableListOf<String>()
                    handler.processSegmentTriggeredJourneys(changedUserIds = null) { _, _, triggerKey ->
                        triggerKeys.add(triggerKey)
                    }

                    triggerKeys shouldBe listOf("55:SEGMENT:COUNT_REACHED:2:1")
                }
            }

            `when`("threshold is not crossed") {
                then("do not execute callback") {
                    val journey =
                        Journey
                            .new(
                                name = "segment-count-journey",
                                triggerType = "SEGMENT",
                                triggerEventName = null,
                                triggerSegmentId = 901L,
                                triggerSegmentEvent = "COUNT_REACHED",
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = 2L,
                                active = true,
                            ).apply { id = 56L }

                    coEvery { journeyRepository.findAllByTriggerTypeAndActiveTrue("SEGMENT") } returns flowOf(journey)
                    coEvery { segmentReadPort.findTargetUserIds(eq(901L)) } returns listOf(1L, 2L)
                    coEvery { journeySegmentCountStateRepository.findByJourneyId(56L) } returns
                        JourneySegmentCountState.new(journeyId = 56L, lastCount = 2L, transitionVersion = 0L)
                    coEvery { journeySegmentCountStateRepository.save(any()) } answers { firstArg() }

                    var called = false
                    handler.processSegmentTriggeredJourneys(changedUserIds = null) { _, _, _ ->
                        called = true
                    }

                    called shouldBe false
                }
            }
        }
    })
