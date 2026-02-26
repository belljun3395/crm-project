package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.Journey
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyRepository : CoroutineCrudRepository<Journey, Long> {
    fun findAllByOrderByCreatedAtDesc(): Flow<Journey>
    fun findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(triggerType: String, triggerEventName: String): Flow<Journey>
}
