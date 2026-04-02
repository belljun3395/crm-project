package com.manage.crm.journey.application

import com.manage.crm.journey.application.dto.BrowseJourneyExecutionHistoryUseCaseIn
import com.manage.crm.journey.domain.JourneyExecutionHistory
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class BrowseJourneyExecutionHistoryUseCaseTest :
    JourneyUnitTestTemplate({
        lateinit var journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository
        lateinit var useCase: BrowseJourneyExecutionHistoryUseCase

        beforeEach {
            journeyExecutionHistoryRepository = mockk()
            useCase = BrowseJourneyExecutionHistoryUseCase(journeyExecutionHistoryRepository = journeyExecutionHistoryRepository)
        }

        given("UC-JOURNEY-005 browse journey execution histories governance anchor") {
            `when`("test suite is discovered") {
                then("keeps UC traceability") {
                }
            }
        }

        given("UC-JOURNEY-005 browse journey execution histories") {
            `when`("histories exist for a journey execution") {
                then("returns ordered history records") {
                    val history =
                        JourneyExecutionHistory
                            .new(
                                journeyExecutionId = 1L,
                                journeyStepId = 201L,
                                status = "SUCCESS",
                                attempt = 1,
                                message = "Action dispatch succeeded",
                                idempotencyKey = null,
                            ).apply { id = 1L }

                    every {
                        journeyExecutionHistoryRepository.findAllByJourneyExecutionIdOrderByCreatedAtAsc(1L)
                    } returns flowOf(history)

                    val result = useCase.execute(BrowseJourneyExecutionHistoryUseCaseIn(journeyExecutionId = 1L))

                    result.histories.size shouldBe 1
                    result.histories[0].id shouldBe 1L
                    result.histories[0].journeyExecutionId shouldBe 1L
                    result.histories[0].status shouldBe "SUCCESS"
                    result.histories[0].attempt shouldBe 1
                }
            }
        }
    })
