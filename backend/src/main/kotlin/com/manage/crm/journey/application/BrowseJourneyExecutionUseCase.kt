package com.manage.crm.journey.application

import com.manage.crm.journey.application.dto.BrowseJourneyExecutionUseCaseIn
import com.manage.crm.journey.application.dto.JourneyExecutionDto
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * UC-JOURNEY-004
 * Reads journey execution records with optional filters.
 *
 * Input: optional journeyId or (eventId + userId) filter tuple.
 * Success: returns execution history rows ordered by creation time descending.
 */
@Component
class BrowseJourneyExecutionUseCase(
    private val journeyExecutionRepository: JourneyExecutionRepository,
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: BrowseJourneyExecutionUseCaseIn): List<JourneyExecutionDto> {
        val executions =
            when {
                useCaseIn.journeyId != null -> journeyExecutionRepository.findAllByJourneyIdOrderByCreatedAtDesc(useCaseIn.journeyId)
                useCaseIn.eventId != null && useCaseIn.userId != null -> {
                    journeyExecutionRepository.findAllByEventIdAndUserIdOrderByCreatedAtDesc(useCaseIn.eventId, useCaseIn.userId)
                }

                useCaseIn.eventId != null || useCaseIn.userId != null -> {
                    throw IllegalArgumentException("eventId and userId must be provided together")
                }

                else -> journeyExecutionRepository.findAllByOrderByCreatedAtDesc()
            }

        return executions.toList().map { execution ->
            JourneyExecutionDto(
                id = requireNotNull(execution.id) { "JourneyExecution id cannot be null" },
                journeyId = execution.journeyId,
                eventId = execution.eventId,
                userId = execution.userId,
                status = execution.status,
                currentStepOrder = execution.currentStepOrder,
                lastError = execution.lastError,
                triggerKey = execution.triggerKey,
                startedAt = execution.startedAt.format(formatter),
                completedAt = execution.completedAt?.format(formatter),
                createdAt = execution.createdAt?.format(formatter) ?: "",
                updatedAt = execution.updatedAt?.format(formatter),
            )
        }
    }
}
