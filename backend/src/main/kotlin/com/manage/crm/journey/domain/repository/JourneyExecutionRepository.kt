package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneyExecution
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyExecutionRepository : CoroutineCrudRepository<JourneyExecution, Long> {
    suspend fun findByTriggerKey(triggerKey: String): JourneyExecution?

    fun findAllByOrderByCreatedAtDesc(): Flow<JourneyExecution>

    fun findAllByJourneyIdOrderByCreatedAtDesc(journeyId: Long): Flow<JourneyExecution>

    fun findAllByEventIdAndUserIdOrderByCreatedAtDesc(
        eventId: Long,
        userId: Long,
    ): Flow<JourneyExecution>
}
