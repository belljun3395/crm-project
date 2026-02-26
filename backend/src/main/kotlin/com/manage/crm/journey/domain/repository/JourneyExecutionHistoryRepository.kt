package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneyExecutionHistory
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyExecutionHistoryRepository : CoroutineCrudRepository<JourneyExecutionHistory, Long> {
    fun findAllByJourneyExecutionIdOrderByCreatedAtAsc(journeyExecutionId: Long): Flow<JourneyExecutionHistory>
}
