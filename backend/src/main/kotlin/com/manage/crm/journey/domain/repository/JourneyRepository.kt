package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.Journey
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Repository for journey definitions and lifecycle transition updates.
 */
interface JourneyRepository :
    CoroutineCrudRepository<Journey, Long>,
    JourneyCustomRepository {
    fun findAllByOrderByCreatedAtDesc(): Flow<Journey>

    fun findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
        triggerType: String,
        triggerEventName: String,
    ): Flow<Journey>

    fun findAllByTriggerTypeAndActiveTrue(triggerType: String): Flow<Journey>
}
