package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.Journey
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyRepository : CoroutineCrudRepository<Journey, Long> {
    fun findAllByOrderByCreatedAtDesc(): Flow<Journey>
    fun findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(triggerType: String, triggerEventName: String): Flow<Journey>
    fun findAllByTriggerTypeAndActiveTrue(triggerType: String): Flow<Journey>

    @Modifying
    @Query(
        """
        UPDATE journeys
        SET lifecycle_status = :lifecycleStatus,
            active = :active,
            version = :newVersion
        WHERE id = :journeyId
          AND version = :expectedVersion
        """
    )
    suspend fun updateLifecycleStatusIfVersionMatches(
        journeyId: Long,
        lifecycleStatus: String,
        active: Boolean,
        expectedVersion: Int,
        newVersion: Int
    ): Int
}
