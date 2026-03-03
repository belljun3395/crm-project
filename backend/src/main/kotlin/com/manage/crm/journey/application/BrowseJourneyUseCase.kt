package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BrowseJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    suspend fun execute(): List<JourneyDto> {
        val journeys = journeyRepository.findAllByOrderByCreatedAtDesc().toList()

        return journeys.map { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

            assembleJourneyDto(journey, steps, objectMapper)
        }
    }
}
