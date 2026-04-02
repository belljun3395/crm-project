package com.manage.crm.journey.application

import com.manage.crm.journey.application.dto.BrowseJourneyExecutionUseCaseIn
import com.manage.crm.journey.domain.JourneyExecution
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseJourneyExecutionUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyExecutionRepository: JourneyExecutionRepository
        lateinit var useCase: BrowseJourneyExecutionUseCase

        beforeEach {
            journeyExecutionRepository = mockk()
            useCase = BrowseJourneyExecutionUseCase(journeyExecutionRepository = journeyExecutionRepository)
        }

        given("UC-JOURNEY-004 browse journey executions governance anchor") {
            `when`("test suite is discovered") {
                then("keeps UC traceability") {
                }
            }
        }

        given("UC-JOURNEY-004 browse journey executions") {
            `when`("no filter provided") {
                then("returns all executions") {
                    val execution =
                        JourneyExecution
                            .new(
                                journeyId = 10L,
                                eventId = 20L,
                                userId = 30L,
                                status = "RUNNING",
                                currentStepOrder = 0,
                                triggerKey = "key-1",
                                startedAt = LocalDateTime.now(),
                            ).apply { id = 1L }

                    every { journeyExecutionRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(execution)

                    val result = useCase.execute(BrowseJourneyExecutionUseCaseIn(journeyId = null, eventId = null, userId = null))

                    result.executions.size shouldBe 1
                    result.executions[0].id shouldBe 1L
                    result.executions[0].journeyId shouldBe 10L
                    result.executions[0].status shouldBe "RUNNING"
                }
            }
        }
    })
