package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchOut
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.journey.application.dto.JourneyAutomationUseCaseIn
import com.manage.crm.journey.application.dto.JourneyExecutionHistoryStatus
import com.manage.crm.journey.application.dto.JourneyExecutionStatus
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyExecution
import com.manage.crm.journey.domain.JourneyExecutionHistory
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.JourneyStepDeduplication
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneySegmentCountStateRepository
import com.manage.crm.journey.domain.repository.JourneySegmentUserStateRepository
import com.manage.crm.journey.domain.repository.JourneyStepDeduplicationRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class JourneyAutomationUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyRepository: JourneyRepository
        lateinit var journeyStepRepository: JourneyStepRepository
        lateinit var journeyExecutionRepository: JourneyExecutionRepository
        lateinit var journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository
        lateinit var journeyStepDeduplicationRepository: JourneyStepDeduplicationRepository
        lateinit var journeySegmentUserStateRepository: JourneySegmentUserStateRepository
        lateinit var journeySegmentCountStateRepository: JourneySegmentCountStateRepository
        lateinit var segmentReadPort: SegmentReadPort
        lateinit var eventReadPort: EventReadPort
        lateinit var actionDispatchService: ActionDispatchService
        lateinit var userReadPort: UserReadPort
        lateinit var useCase: JourneyAutomationUseCase

        beforeTest {
            journeyRepository = mockk()
            journeyStepRepository = mockk()
            journeyExecutionRepository = mockk()
            journeyExecutionHistoryRepository = mockk()
            journeyStepDeduplicationRepository = mockk()
            journeySegmentUserStateRepository = mockk()
            journeySegmentCountStateRepository = mockk()
            segmentReadPort = mockk()
            eventReadPort = mockk()
            actionDispatchService = mockk()
            userReadPort = mockk()

            useCase =
                JourneyAutomationUseCase(
                    journeyRepository = journeyRepository,
                    journeyStepRepository = journeyStepRepository,
                    journeyExecutionRepository = journeyExecutionRepository,
                    journeyExecutionHistoryRepository = journeyExecutionHistoryRepository,
                    journeyStepDeduplicationRepository = journeyStepDeduplicationRepository,
                    journeySegmentUserStateRepository = journeySegmentUserStateRepository,
                    journeySegmentCountStateRepository = journeySegmentCountStateRepository,
                    segmentReadPort = segmentReadPort,
                    eventReadPort = eventReadPort,
                    actionDispatchService = actionDispatchService,
                    userReadPort = userReadPort,
                    objectMapper = ObjectMapper(),
                )

            coEvery { journeyRepository.findAllByTriggerTypeAndActiveTrue(JourneyTriggerType.CONDITION.name) } returns emptyFlow()

            coEvery { userReadPort.findByExternalId(any()) } returns null
            coEvery { userReadPort.findAll() } returns emptyList()
            coEvery { userReadPort.findAllByIdIn(any()) } answers {
                val ids = firstArg<Collection<Long>>()
                ids.map { id ->
                    UserReadModel(
                        id = id,
                        externalId = "user-$id",
                        userAttributesJson = """{"email":"user$id@example.com","name":"tester"}""",
                        createdAt = LocalDateTime.of(2026, 2, 25, 9, 0, 0),
                        updatedAt = LocalDateTime.of(2026, 2, 25, 9, 0, 0),
                    )
                }
            }
        }

        afterTest { (_, _) ->
            clearMocks(
                journeyRepository,
                journeyStepRepository,
                journeyExecutionRepository,
                journeyExecutionHistoryRepository,
                journeyStepDeduplicationRepository,
                journeySegmentUserStateRepository,
                journeySegmentCountStateRepository,
                segmentReadPort,
                eventReadPort,
                actionDispatchService,
                userReadPort,
            )
        }

        given("UC-JOURNEY-007 JourneyAutomationUseCase.onEvent") {
            `when`("journey has executable action step") {
                then("dispatch action and mark execution as success") {
                    val event = newEvent()
                    val journey =
                        Journey
                            .new(
                                name = "welcome-journey",
                                triggerType = JourneyTriggerType.EVENT.name,
                                triggerEventName = "purchase",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = true,
                            ).apply { id = 1L }

                    val step =
                        JourneyStep
                            .new(
                                journeyId = 1L,
                                stepOrder = 1,
                                stepType = JourneyStepType.ACTION.name,
                                channel = ActionChannel.SLACK.name,
                                destination = "https://hooks.slack.com/services/T000/B000/XXX",
                                subject = "notice",
                                body = "hello {{eventName}}",
                                variablesJson = "{}",
                                delayMillis = null,
                                conditionExpression = null,
                                retryCount = 0,
                            ).apply {
                                id = 10L
                            }

                    coEvery {
                        journeyRepository.findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
                            JourneyTriggerType.EVENT.name,
                            "purchase",
                        )
                    } returns flowOf(journey)
                    coEvery { journeyExecutionRepository.findByTriggerKey("1:100:1") } returns null
                    coEvery { journeyExecutionRepository.save(any()) } answers {
                        firstArg<JourneyExecution>().apply {
                            if (id == null) {
                                id = 500L
                            }
                        }
                    }
                    coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(1L) } returns flowOf(step)
                    coEvery { journeyStepDeduplicationRepository.save(any()) } returns JourneyStepDeduplication.new("1")
                    coEvery { actionDispatchService.dispatch(any()) } returns
                        ActionDispatchOut(
                            status = ActionDispatchStatus.SUCCESS,
                            channel = ActionChannel.SLACK,
                            destination = step.destination!!,
                            providerMessageId = "msg-1",
                        )
                    coEvery { journeyExecutionHistoryRepository.save(any()) } answers {
                        firstArg<JourneyExecutionHistory>().apply {
                            if (id == null) {
                                id = 1L
                            }
                        }
                    }

                    useCase.execute(JourneyAutomationUseCaseIn(event = event))

                    coVerify(exactly = 1) { actionDispatchService.dispatch(any()) }
                    val executionSlot = mutableListOf<JourneyExecution>()
                    coVerify(atLeast = 1) { journeyExecutionRepository.save(capture(executionSlot)) }
                    executionSlot.any {
                        it.status == JourneyExecutionStatus.SUCCESS.name &&
                            it.journeyId == 1L &&
                            it.eventId == 100L
                    } shouldBe true

                    val historySavedSlot = mutableListOf<JourneyExecutionHistory>()
                    coVerify(atLeast = 1) { journeyExecutionHistoryRepository.save(capture(historySavedSlot)) }
                    historySavedSlot.any { it.status == JourneyExecutionHistoryStatus.SUCCESS.name } shouldBe true
                }
            }

            `when`("step idempotency key is duplicated") {
                then("skip duplicated action dispatch") {
                    val event = newEvent()
                    val journey =
                        Journey
                            .new(
                                name = "welcome-journey",
                                triggerType = JourneyTriggerType.EVENT.name,
                                triggerEventName = "purchase",
                                triggerSegmentId = null,
                                triggerSegmentEvent = null,
                                triggerSegmentWatchFields = null,
                                triggerSegmentCountThreshold = null,
                                active = true,
                            ).apply { id = 1L }

                    val step =
                        JourneyStep
                            .new(
                                journeyId = 1L,
                                stepOrder = 1,
                                stepType = JourneyStepType.ACTION.name,
                                channel = ActionChannel.DISCORD.name,
                                destination = "https://discord.com/api/webhooks/1/2",
                                subject = null,
                                body = "hello",
                                variablesJson = "{}",
                                delayMillis = null,
                                conditionExpression = null,
                                retryCount = 0,
                            ).apply {
                                id = 11L
                            }

                    coEvery {
                        journeyRepository.findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
                            JourneyTriggerType.EVENT.name,
                            "purchase",
                        )
                    } returns flowOf(journey)
                    coEvery { journeyExecutionRepository.findByTriggerKey("1:100:1") } returns null
                    coEvery { journeyExecutionRepository.save(any()) } answers {
                        firstArg<JourneyExecution>().apply {
                            if (id == null) {
                                id = 501L
                            }
                        }
                    }
                    coEvery { journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(1L) } returns flowOf(step)
                    coEvery {
                        journeyStepDeduplicationRepository.save(any())
                    } throws DataIntegrityViolationException("duplicate key")
                    coEvery { journeyExecutionHistoryRepository.save(any()) } answers {
                        firstArg<JourneyExecutionHistory>().apply {
                            if (id == null) {
                                id = 2L
                            }
                        }
                    }

                    useCase.execute(JourneyAutomationUseCaseIn(event = event))

                    coVerify(exactly = 0) { actionDispatchService.dispatch(any()) }

                    val historySlot = slot<JourneyExecutionHistory>()
                    coVerify {
                        journeyExecutionHistoryRepository.save(capture(historySlot))
                    }
                    historySlot.captured.status shouldBe JourneyExecutionHistoryStatus.SKIPPED_DUPLICATE.name
                }
            }
        }
    }) {
    companion object {
        private fun newEvent(): Event =
            Event.new(
                id = 100L,
                name = "purchase",
                userId = 1L,
                properties =
                    EventProperties(
                        listOf(
                            EventProperty("amount", "120"),
                            EventProperty("plan", "starter"),
                        ),
                    ),
                createdAt = LocalDateTime.of(2026, 2, 25, 10, 0, 0),
            )
    }
}
