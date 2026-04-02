package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.BrowseJourneyUseCaseIn
import com.manage.crm.journey.application.dto.BrowseJourneyUseCaseOut
import com.manage.crm.journey.application.dto.toJourneyDto
import com.manage.crm.journey.application.dto.toJourneyStepDto
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/**
 * UC-JOURNEY-003
 * Returns journeys with expanded step payloads ordered by creation time.
 *
 * Input: browse query wrapper with optional limit (1–200, default 50).
 * Success: returns journey list with step payloads assembled for API consumption.
 */
@Component
class BrowseJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseJourneyUseCaseIn): BrowseJourneyUseCaseOut {
        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val journeys =
            journeyRepository
                .findAllByOrderByCreatedAtDesc()
                .take(normalizedLimit)
                .toList()

        val journeyIds = journeys.mapNotNull { it.id }
        val stepsByJourneyId =
            if (journeyIds.isEmpty()) {
                emptyMap()
            } else {
                journeyStepRepository
                    .findAllByJourneyIdInOrderByJourneyIdAscStepOrderAsc(journeyIds)
                    .toList()
                    .groupBy { it.journeyId }
            }

        return BrowseJourneyUseCaseOut(
            journeys.map { journey ->
                val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
                val steps = stepsByJourneyId[journeyId].orEmpty()
                journey.toJourneyDto(steps.map { it.toJourneyStepDto(objectMapper) }, objectMapper)
            },
        )
    }
}
