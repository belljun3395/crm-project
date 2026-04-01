package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneyExecutionHistory
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Repository for execution step history records used in journey run tracing and mutation guards.
 */
interface JourneyExecutionHistoryRepository : CoroutineCrudRepository<JourneyExecutionHistory, Long> {
    fun findAllByJourneyExecutionIdOrderByCreatedAtAsc(journeyExecutionId: Long): Flow<JourneyExecutionHistory>

    suspend fun existsByJourneyStepId(journeyStepId: Long): Boolean
}
