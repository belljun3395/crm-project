package com.manage.crm.journey.application

import com.manage.crm.journey.application.dto.BrowseJourneyExecutionHistoryUseCaseIn
import com.manage.crm.journey.application.dto.JourneyExecutionHistoryDto
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * UC-JOURNEY-005
 * Reads step-level execution histories for a journey execution.
 *
 * Input: journey execution id.
 * Success: returns ordered step history records for timeline inspection.
 */
@Component
class BrowseJourneyExecutionHistoryUseCase(
    private val journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository,
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: BrowseJourneyExecutionHistoryUseCaseIn): List<JourneyExecutionHistoryDto> =
        journeyExecutionHistoryRepository
            .findAllByJourneyExecutionIdOrderByCreatedAtAsc(useCaseIn.journeyExecutionId)
            .toList()
            .map { history ->
                JourneyExecutionHistoryDto(
                    id = requireNotNull(history.id) { "JourneyExecutionHistory id cannot be null" },
                    journeyExecutionId = history.journeyExecutionId,
                    journeyStepId = history.journeyStepId,
                    status = history.status,
                    attempt = history.attempt,
                    message = history.message,
                    idempotencyKey = history.idempotencyKey,
                    createdAt = history.createdAt?.format(formatter) ?: "",
                )
            }
}
