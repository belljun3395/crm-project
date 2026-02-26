package com.manage.crm.action.domain.repository

import com.manage.crm.action.domain.ActionDispatchHistory
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ActionDispatchHistoryRepository : CoroutineCrudRepository<ActionDispatchHistory, Long> {
    fun findAllByOrderByCreatedAtDesc(): Flow<ActionDispatchHistory>
    fun findAllByCampaignIdOrderByCreatedAtDesc(campaignId: Long): Flow<ActionDispatchHistory>
    fun findAllByJourneyExecutionIdOrderByCreatedAtDesc(journeyExecutionId: Long): Flow<ActionDispatchHistory>
}
