package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.BrowseJourneyUseCaseIn
import com.manage.crm.journey.application.dto.JourneyDto
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/**
 * UC-JOURNEY-003
 * Returns journeys with expanded step payloads ordered by creation time.
 *
 * Input: browse query wrapper.
 * Success: returns journey list with step payloads assembled for API consumption.
 */
@Component
class BrowseJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper,
) {
    suspend fun execute(useCaseIn: BrowseJourneyUseCaseIn): List<JourneyDto> {
        val journeys = journeyRepository.findAllByOrderByCreatedAtDesc().toList()

        return journeys.map { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

            assembleJourneyDto(journey, steps, objectMapper)
        }
    }
}
