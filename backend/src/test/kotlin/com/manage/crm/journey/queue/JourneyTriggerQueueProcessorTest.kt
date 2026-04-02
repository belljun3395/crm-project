package com.manage.crm.journey.queue

import com.manage.crm.journey.application.JourneyAutomationUseCase
import com.manage.crm.journey.application.dto.JourneyAutomationUseCaseIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime

class JourneyTriggerQueueProcessorTest :
    BehaviorSpec({
        lateinit var useCase: JourneyAutomationUseCase
        lateinit var processor: JourneyTriggerQueueProcessor

        beforeEach {
            useCase = mockk()
            processor = JourneyTriggerQueueProcessor(useCase)
            coEvery { useCase.execute(any()) } returns Unit
        }

        given("EVENT trigger message") {
            `when`("processor routes to automation use case") {
                then("it passes event payload as JourneyAutomationUseCaseIn") {
                    val message =
                        JourneyTriggerQueueMessage(
                            triggerType = JourneyTriggerQueueType.EVENT,
                            event =
                                JourneyEventPayload(
                                    id = 11L,
                                    name = "purchase",
                                    userId = 101L,
                                    properties = listOf(JourneyEventPropertyPayload("plan", "pro")),
                                    createdAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0),
                                ),
                        )

                    processor.process(message)

                    val inputSlot = slot<JourneyAutomationUseCaseIn>()
                    coVerify(exactly = 1) { useCase.execute(capture(inputSlot)) }
                    inputSlot.captured.event?.id shouldBe 11L
                    inputSlot.captured.event?.name shouldBe "purchase"
                    inputSlot.captured.event?.userId shouldBe 101L
                    inputSlot.captured.event
                        ?.properties shouldBe mapOf("plan" to "pro")
                    inputSlot.captured.changedUserIds shouldBe null
                }
            }

            `when`("event payload is null") {
                then("it throws IllegalArgumentException") {
                    val message = JourneyTriggerQueueMessage(triggerType = JourneyTriggerQueueType.EVENT, event = null)

                    shouldThrow<IllegalArgumentException> {
                        processor.process(message)
                    }
                }
            }
        }

        given("SEGMENT_CONTEXT trigger message") {
            `when`("processor routes changed user ids") {
                then("it passes changedUserIds without event") {
                    val message =
                        JourneyTriggerQueueMessage(
                            triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT,
                            changedUserIds = listOf(1L, 2L, 3L),
                        )

                    processor.process(message)

                    val inputSlot = slot<JourneyAutomationUseCaseIn>()
                    coVerify(exactly = 1) { useCase.execute(capture(inputSlot)) }
                    inputSlot.captured.event shouldBe null
                    inputSlot.captured.changedUserIds shouldContainExactly listOf(1L, 2L, 3L)
                }
            }
        }
    })
