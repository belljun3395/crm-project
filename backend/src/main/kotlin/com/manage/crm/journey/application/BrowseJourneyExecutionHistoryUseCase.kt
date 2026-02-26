package com.manage.crm.journey.application

import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class BrowseJourneyExecutionHistoryUseCase(
    private val journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(journeyExecutionId: Long): List<JourneyExecutionHistoryDto> {
        return journeyExecutionHistoryRepository
            .findAllByJourneyExecutionIdOrderByCreatedAtAsc(journeyExecutionId)
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
                    createdAt = history.createdAt?.format(formatter) ?: ""
                )
            }
    }
}
